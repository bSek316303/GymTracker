package org.key0.gymtracker.controllers;


import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.dto.WorkoutPlanDto;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.PlanExerciseRepository;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WorkoutPlanRepository;
import org.key0.gymtracker.services.PlanService;
import org.key0.gymtracker.services.UserService;
import org.key0.gymtracker.services.WorkoutService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/plan")
@AllArgsConstructor
public class PlanController {

    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final UserService userService;
    private final WorkoutService workoutService;
    private final PlanService planService;

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

                    TrackingParameter defaultTrackingParameter = TrackingParameter.REPETITIONS;

                    PlanExercise defaultExercise = new PlanExercise();
                    defaultExercise.setPlan(savedPlan);
                    defaultExercise.setExerciseName("Przykładowe ćwiczenie");
                    defaultExercise.setTargetSets(3);
                    defaultExercise.setTrackingParameter(defaultTrackingParameter);
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
                    pe.getTrackingParameter().name(),
                    pe.getNotes(),
                    pe.getDayNumber(),
                    pe.getExerciseNumber()
            ));
        }

        httpSession.setAttribute("workoutPlanDto", workoutPlanDto);
        model.addAttribute("workoutPlanDto", workoutPlanDto);
        model.addAttribute("currentDay", null);
        model.addAttribute("trackingParameters", TrackingParameter.values());

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
        model.addAttribute("trackingParameters", TrackingParameter.values());

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
            planService.ensureExerciseList(workoutPlanDto);
            long exerciseCount = workoutPlanDto.getPlanExerciseList().stream()
                    .filter(e -> e.getDayNumber() == day)
                    .count();

            workoutPlanDto.getPlanExerciseList().add(
                    new PlanExerciseDto("", 3, "REPETITIONS",  "", day, (int) (exerciseCount + 1))
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

        try {
            User user = userService.getUser(currentUser);
            WorkoutPlan plan = workoutService.getWorkoutPlan(user);

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
                planService.ensureExerciseList(currentSessionDto);
                currentSessionDto.setDaysPerWeek(updatedPlanDto.getDaysPerWeek());
                currentSessionDto.getPlanExerciseList().removeIf(ex -> ex.getDayNumber() > updatedPlanDto.getDaysPerWeek());
                httpSession.setAttribute("workoutPlanDto", currentSessionDto);
            }

            if (day != null && day <= updatedPlanDto.getDaysPerWeek()) {
                return "redirect:/plan/plan-creator/" + day;
            }

        } catch (Exception e){
            return "error";
        }
        return "redirect:/plan/plan-creator";
    }

    @PostMapping("/plan-creator/remove-exercise/{day}/{exerciseNumber}")
    public String removeExerciseFromDay(@PathVariable("day") Integer day,
                                        @PathVariable("exerciseNumber") Integer exerciseNumber,
                                        @ModelAttribute("formDto") WorkoutPlanDto updatedPlanDto,
                                        HttpSession httpSession) {
        updateSessionFromForm(day, updatedPlanDto, httpSession);

        WorkoutPlanDto workoutPlanDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        if (workoutPlanDto != null && workoutPlanDto.getPlanExerciseList() != null) {
            workoutPlanDto.getPlanExerciseList().removeIf(ex -> ex.getDayNumber() == day && ex.getExerciseNumber() == exerciseNumber);

            workoutPlanDto.getPlanExerciseList().stream()
                    .filter(ex -> ex.getDayNumber() == day && ex.getExerciseNumber() > exerciseNumber)
                    .forEach(ex -> ex.setExerciseNumber(ex.getExerciseNumber() - 1));

            httpSession.setAttribute("workoutPlanDto", workoutPlanDto);
        }

        return "redirect:/plan/plan-creator/" + day;
    }

    @PostMapping(value = {"/plan-creator/save", "/plan-creator/{day}/save"})
    public String savePlan(@PathVariable(value = "day", required = false) Integer day,
                           @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                           HttpSession httpSession,
                           @AuthenticationPrincipal UserDetails currentUser) {

        try {
            if (day != null) {
                updateSessionFromForm(day, updatedPlanDto, httpSession);
            }

            User user = userService.getUser(currentUser);
            WorkoutPlan plan = workoutService.getWorkoutPlan(user);

            WorkoutPlanDto currentSessionDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");
            if (currentSessionDto == null) {
                return "redirect:/plan/plan-creator";
            }

            planService.shrinkPlanIfNeeded(currentSessionDto);

            List<PlanExercise> userOldExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
            planExerciseRepository.deleteAll(userOldExercises);

            plan.setDaysPerWeek(currentSessionDto.getDaysPerWeek());
            workoutPlanRepository.save(plan);

            List<PlanExercise> newExercises = currentSessionDto.getPlanExerciseList().stream()
                    .map(peDto -> {
                        PlanExercise pe = peDto.toPlanExerciseWithoutPlan();
                        pe.setPlan(plan);
                        return pe;
                    }).toList();

            planExerciseRepository.saveAll(newExercises);

            httpSession.removeAttribute("workoutPlanDto");
        } catch (Exception e){
            System.out.println(e.getMessage());
            return "error";
        }

        return "redirect:/plan";
    }

    private void updateSessionFromForm(Integer day, WorkoutPlanDto updatedPlanDto, HttpSession httpSession) {
        WorkoutPlanDto currentSessionDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        if (currentSessionDto != null) {
            planService.ensureExerciseList(currentSessionDto);
            if (day != null) {
                if (updatedPlanDto == null || updatedPlanDto.getPlanExerciseList() == null || updatedPlanDto.getPlanExerciseList().isEmpty()) {
                    return;
                }

                currentSessionDto.getPlanExerciseList().removeIf(ex -> day.equals(ex.getDayNumber()));

                if (updatedPlanDto.getPlanExerciseList() != null) {

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
            currentSessionDto.getPlanExerciseList().forEach(ex -> ex.setTrackingParameter(
                    parseTrackingParameter(ex.getTrackingParameter()).name()
            ));

            httpSession.setAttribute("workoutPlanDto", currentSessionDto);
        }
    }

    private TrackingParameter parseTrackingParameter(String trackingParameter) {
        if (trackingParameter == null || trackingParameter.isBlank()) {
            return TrackingParameter.REPETITIONS;
        }

        try {
            return TrackingParameter.fromString(trackingParameter);
        } catch (IllegalArgumentException ex) {
            return TrackingParameter.REPETITIONS;
        }
    }
}
