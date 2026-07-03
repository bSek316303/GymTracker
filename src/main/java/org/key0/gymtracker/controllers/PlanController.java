package org.key0.gymtracker.controllers;


import jakarta.servlet.http.HttpSession;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.dto.WorkoutPlanDto;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.PlanExerciseRepository;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WorkoutPlanRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/plan")
public class PlanController {

    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanExerciseRepository planExerciseRepository;

    public PlanController(UserRepository userRepository, WorkoutPlanRepository workoutPlanRepository, PlanExerciseRepository planExerciseRepository){
        this.userRepository = userRepository;
        this.workoutPlanRepository = workoutPlanRepository;
        this.planExerciseRepository = planExerciseRepository;
    }

    @GetMapping("/plan-creator")
    public String planCreator(HttpSession httpSession, Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + currentUser.getUsername()));

        // Szukamy planu w bazie
        WorkoutPlan plan = workoutPlanRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    // JEŚLI NIE MA PLANU: Tworzymy i od razu zapisujemy bazowy szablon w bazie danych!
                    WorkoutPlan newPlan = new WorkoutPlan();
                    newPlan.setUser(user);
                    newPlan.setDaysPerWeek(3);
                    WorkoutPlan savedPlan = workoutPlanRepository.save(newPlan);

                    // Dodajemy domyślne, puste ćwiczenie na start do Dnia 1
                    PlanExercise defaultExercise = new PlanExercise();
                    defaultExercise.setPlan(savedPlan);
                    defaultExercise.setExerciseName("Przykładowe ćwiczenie");
                    defaultExercise.setTargetSets(3);
                    defaultExercise.setNotes("Zmień nazwę i serie");
                    defaultExercise.setDayNumber(1);
                    defaultExercise.setExerciseNumber(1);
                    planExerciseRepository.save(defaultExercise);

                    return savedPlan;
                });

        // W tym miejscu mamy PEWNOŚĆ, że plan istnieje w bazie (nowy lub stary).
        // Mapujemy go do DTO do sesji tak jak wcześniej...
        List<PlanExercise> planExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
        WorkoutPlanDto workoutPlanDto = new WorkoutPlanDto();
        workoutPlanDto.setDaysPerWeek(plan.getDaysPerWeek());
        workoutPlanDto.setPlanExerciseList(new ArrayList<>());

        for (PlanExercise pe : planExercises) {
            workoutPlanDto.getPlanExerciseList().add(new PlanExerciseDto(
                    pe.getExerciseName(),
                    pe.getTargetSets(),
                    pe.getNotes(),
                    pe.getDayNumber(),
                    pe.getExerciseNumber()
            ));
        }

        httpSession.setAttribute("workoutPlanDto", workoutPlanDto);
        model.addAttribute("workoutPlanDto", workoutPlanDto);
        model.addAttribute("currentDay", null);

        return "plan_creator";
    }

    @GetMapping("/plan-creator/{day}")
    public String planCreatorForDay(@PathVariable("day") int day, HttpSession httpSession, Model model) {
        WorkoutPlanDto workoutPlanDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        model.addAttribute("workoutPlanDto", workoutPlanDto);
        model.addAttribute("currentDay", day);

        return "plan_creator";
    }

    @PostMapping("/plan-creator/{currentDay}/switch-day/{targetDay}")
    public String switchDayAndSave(
            @PathVariable("currentDay") int currentDay,
            @PathVariable("targetDay") int targetDay,
            @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
            HttpSession httpSession) {

        updateSessionFromForm(currentDay, updatedPlanDto, httpSession);

        return "redirect:/plan/plan-creator/" + targetDay;
    }

    @PostMapping("/plan-creator/{day}/add-exercise")
    public String addExerciseToDay(@PathVariable("day") Integer day,
                                   @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                                   HttpSession httpSession) {
        WorkoutPlanDto workoutPlanDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        updateSessionFromForm(day, updatedPlanDto, httpSession);

        if (workoutPlanDto != null) {
            long exerciseCount = workoutPlanDto.getPlanExerciseList().stream()
                    .filter(e -> e.getDayNumber() == day)
                    .count();

            workoutPlanDto.getPlanExerciseList().add(
                    new PlanExerciseDto("", 3, "", day, (int) (exerciseCount + 1))
            );

            httpSession.setAttribute("workoutPlanDto", workoutPlanDto);
        }

        return "redirect:/plan/plan-creator/" + day;
    }

    @PostMapping(value = {"/plan-creator/change-days", "/plan-creator/{day}/change-days"})
    public String changeDays(@PathVariable(value = "day", required = false) Integer day,
                             @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                             HttpSession httpSession,
                             @AuthenticationPrincipal UserDetails currentUser) {

        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        WorkoutPlan plan = workoutPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono planu w bazie"));

        plan.setDaysPerWeek(updatedPlanDto.getDaysPerWeek());
        workoutPlanRepository.save(plan);

        List<PlanExercise> planExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
        for (PlanExercise pe : planExercises) {
            if (pe.getDayNumber() > updatedPlanDto.getDaysPerWeek()) {
                planExerciseRepository.delete(pe);
            }
        }

        WorkoutPlanDto currentSessionDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");
        if (currentSessionDto != null) {
            currentSessionDto.setDaysPerWeek(updatedPlanDto.getDaysPerWeek());
            currentSessionDto.getPlanExerciseList().removeIf(ex -> ex.getDayNumber() > updatedPlanDto.getDaysPerWeek());
            httpSession.setAttribute("workoutPlanDto", currentSessionDto);
        }

        if (day != null && day <= updatedPlanDto.getDaysPerWeek()) {
            return "redirect:/plan/plan-creator/" + day;
        }

        return "redirect:/plan/plan-creator";
    }

    @PostMapping(value = {"/plan-creator/save", "/plan-creator/{day}/save"})
    public String savePlan(@PathVariable(value = "day", required = false) Integer day,
                           @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                           HttpSession httpSession,
                           @AuthenticationPrincipal UserDetails currentUser) {

        updateSessionFromForm(day, updatedPlanDto, httpSession);

        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        WorkoutPlan plan = workoutPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono planu w bazie"));

        WorkoutPlanDto currentSessionDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        planExerciseRepository.deleteAll();

        currentSessionDto.getPlanExerciseList().stream().forEach(peDto -> {
            PlanExercise pe = new PlanExercise();
            pe.setDayNumber(peDto.getDayNumber());
            pe.setExerciseNumber(peDto.getExerciseNumber());
            pe.setExerciseName(peDto.getExerciseName());
            pe.setTargetSets(peDto.getTargetSets());
            pe.setNotes(peDto.getNotes());
            pe.setPlan(plan);
            planExerciseRepository.save(pe);
        });

        return "redirect:/plan";
    }

    private void updateSessionFromForm(Integer day, WorkoutPlanDto updatedPlanDto, HttpSession httpSession) {
        WorkoutPlanDto currentSessionDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        if (currentSessionDto != null) {
            if (day != null) {
                // Czyścimy stary stan dla tego konkretnego dnia i dodajemy nowy z formularza
                currentSessionDto.getPlanExerciseList().removeIf(ex -> ex.getDayNumber() == day);
                if (updatedPlanDto.getPlanExerciseList() != null) {
                    currentSessionDto.getPlanExerciseList().addAll(updatedPlanDto.getPlanExerciseList());
                }
            } else {
                // Aktualizacja ogólna (np. zmiana liczby dni)
                currentSessionDto.setDaysPerWeek(updatedPlanDto.getDaysPerWeek());
                if (updatedPlanDto.getPlanExerciseList() != null) {
                    currentSessionDto.setPlanExerciseList(updatedPlanDto.getPlanExerciseList());
                }
            }
            // Czyszczenie ćwiczeń wykraczających poza limit dni
            currentSessionDto.getPlanExerciseList().removeIf(ex -> ex.getDayNumber() > currentSessionDto.getDaysPerWeek());

            // Zapisujemy zaktualizowany "koszyk" z powrotem do sesji
            httpSession.setAttribute("workoutPlanDto", currentSessionDto);
        }
    }
}
