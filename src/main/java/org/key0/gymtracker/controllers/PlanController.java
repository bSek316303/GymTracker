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

    @PostMapping("/{day}")
    public String viewExercises(Model model, @AuthenticationPrincipal UserDetails currentUser, @PathVariable Integer day, HttpSession httpSession){
        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + currentUser.getUsername()));

        WorkoutPlan plan = workoutPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono planu w bazie"));

        model.addAttribute("exercises", planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId()).stream().
                filter(pe -> day.equals(pe.getDayNumber())));

        httpSession.setAttribute("currentDay", day);
        return "redirect:/plan";
    }

    @GetMapping("/plan-creator")
    public String planCreator(HttpSession httpSession, Model model, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + currentUser.getUsername()));

        WorkoutPlan plan = workoutPlanRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    WorkoutPlan newPlan = new WorkoutPlan();
                    newPlan.setUser(user);
                    newPlan.setDaysPerWeek(3);
                    WorkoutPlan savedPlan = workoutPlanRepository.save(newPlan);

                    PlanExercise defaultExercise = new PlanExercise();
                    defaultExercise.setPlan(savedPlan);
                    defaultExercise.setExerciseName("Przykładowe ćwiczenie");
                    defaultExercise.setTargetSets(3);
                    defaultExercise.setNotes("Zmień nazwę i serie, notatki są opcjonalne i pomagają zapamiętać ważne informacje odnośnie ćwiczenia");
                    defaultExercise.setDayNumber(1);
                    defaultExercise.setExerciseNumber(1);
                    planExerciseRepository.save(defaultExercise);

                    return savedPlan;
                });

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
        if (workoutPlanDto == null) {
            return "redirect:/plan/plan-creator";
        }

        model.addAttribute("workoutPlanDto", workoutPlanDto);
        model.addAttribute("currentDay", day);

        return "plan_creator";
    }

    @PostMapping("/plan-creator/{currentDay}/switch-day/{targetDay}")
    public String switchDayAndSave(
            @PathVariable("currentDay") int currentDay,
            @PathVariable("targetDay") int targetDay,
            @ModelAttribute("formDto") WorkoutPlanDto updatedPlanDto,
            HttpSession httpSession) {

        updateSessionFromForm(currentDay, updatedPlanDto, httpSession);

        return "redirect:/plan/plan-creator/" + targetDay;
    }

    @PostMapping("/plan-creator/{day}/add-exercise")
    public String addExerciseToDay(@PathVariable("day") Integer day,
                                   @ModelAttribute("formDto") WorkoutPlanDto updatedPlanDto,
                                   HttpSession httpSession) {
        WorkoutPlanDto workoutPlanDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        updateSessionFromForm(day, updatedPlanDto, httpSession);

        if (workoutPlanDto != null) {
            ensureExerciseList(workoutPlanDto);
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
                             @ModelAttribute("formDto") WorkoutPlanDto updatedPlanDto,
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
            ensureExerciseList(currentSessionDto);
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
        if (currentSessionDto == null) {
            return "redirect:/plan/plan-creator";
        }
        ensureExerciseList(currentSessionDto);

        int totalDays = currentSessionDto.getDaysPerWeek();
        for (int currentDayCheck = 1; currentDayCheck <= totalDays; currentDayCheck++) {

            final int emptyDayCandidate = currentDayCheck;

            boolean hasExercises = currentSessionDto.getPlanExerciseList().stream()
                    .anyMatch(pe -> pe.getDayNumber() == emptyDayCandidate);

            if (!hasExercises) {

                currentSessionDto.getPlanExerciseList().stream()
                        .filter(pe -> pe.getDayNumber() > emptyDayCandidate)
                        .forEach(pe -> pe.setDayNumber(pe.getDayNumber() - 1));

                totalDays--;
                currentSessionDto.setDaysPerWeek(totalDays);

                currentDayCheck--;
            }
        }

        List<PlanExercise> userOldExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
        planExerciseRepository.deleteAll(userOldExercises);

        plan.setDaysPerWeek(currentSessionDto.getDaysPerWeek());
        workoutPlanRepository.save(plan);

        currentSessionDto.getPlanExerciseList().forEach(peDto -> {
            PlanExercise pe = new PlanExercise();
            pe.setDayNumber(peDto.getDayNumber());
            pe.setExerciseNumber(peDto.getExerciseNumber());
            pe.setExerciseName(peDto.getExerciseName());
            pe.setTargetSets(peDto.getTargetSets());
            pe.setNotes(peDto.getNotes());
            pe.setPlan(plan);
            planExerciseRepository.save(pe);
        });

        httpSession.removeAttribute("workoutPlanDto");

        return "redirect:/plan";
    }

    private void updateSessionFromForm(Integer day, WorkoutPlanDto updatedPlanDto, HttpSession httpSession) {
        WorkoutPlanDto currentSessionDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        if (currentSessionDto != null) {
            ensureExerciseList(currentSessionDto);
            if (day != null) {
                if (updatedPlanDto == null || updatedPlanDto.getPlanExerciseList() == null || updatedPlanDto.getPlanExerciseList().isEmpty()) {
                    return;
                }

                currentSessionDto.getPlanExerciseList().removeIf(ex -> day.equals(ex.getDayNumber()));

                if (updatedPlanDto != null && updatedPlanDto.getPlanExerciseList() != null) {

                    List<PlanExerciseDto> validExercisesFromForm = updatedPlanDto.getPlanExerciseList().stream()
                            .filter(ex -> ex != null)
                            .filter(ex -> ex.getExerciseName() != null && !ex.getExerciseName().trim().isEmpty())
                            .filter(ex -> day.equals(ex.getDayNumber()))
                            .toList();

                    currentSessionDto.getPlanExerciseList().addAll(validExercisesFromForm);
                }
            } else {
                currentSessionDto.setDaysPerWeek(updatedPlanDto.getDaysPerWeek());
                if (updatedPlanDto.getPlanExerciseList() != null) {
                    List<PlanExerciseDto> cleanList = updatedPlanDto.getPlanExerciseList().stream()
                            .filter(ex -> ex != null && ex.getExerciseName() != null && !ex.getExerciseName().trim().isEmpty())
                            .toList();
                    currentSessionDto.setPlanExerciseList(new ArrayList<>(cleanList));
                }
            }

            // Czyszczenie na wypadek zmniejszenia liczby dni w konfiguratorze
            currentSessionDto.getPlanExerciseList().removeIf(ex -> ex.getDayNumber() > currentSessionDto.getDaysPerWeek());

            httpSession.setAttribute("workoutPlanDto", currentSessionDto);
        }
    }

    private void ensureExerciseList(WorkoutPlanDto workoutPlanDto) {
        if (workoutPlanDto.getPlanExerciseList() == null) {
            workoutPlanDto.setPlanExerciseList(new ArrayList<>());
        }
    }
}
