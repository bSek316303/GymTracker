package org.key0.gymtracker.controllers;


import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.ExerciseResultDto;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.dto.WorkoutPlanDto;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.PlanExerciseRepository;
import org.key0.gymtracker.repositories.WorkoutPlanRepository;
import org.key0.gymtracker.services.UserService;
import org.key0.gymtracker.services.PlanService;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/plan")
@AllArgsConstructor
public class PlanController {
    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final UserService userService;
    private final PlanService planService;

    @PostMapping("/{day}")
    public String viewExercises(Model model, @AuthenticationPrincipal UserDetails currentUser, @PathVariable Integer day){
        try {
            User user = userService.getUser(currentUser);
            Optional<WorkoutPlan> optionalWorkoutPlan = workoutPlanRepository.findByUserId(user.getId());

            if(optionalWorkoutPlan.isEmpty()) {
                model.addAttribute("workoutPlan", null);
                model.addAttribute("exercises", null);
            }
            else {
                model.addAttribute("workoutPlan", optionalWorkoutPlan.get());
                model.addAttribute("exercises", planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(optionalWorkoutPlan.get().getId()).stream().
                        filter(pe -> day.equals(pe.getDayNumber())));
            }

            model.addAttribute("currentDay", day);
        } catch (Exception e){
            model.addAttribute("message", e.getMessage());
            return "error";
        }
        return "plan";
    }

    @GetMapping("/plan-creator")
    public String planCreator(HttpSession httpSession, Model model, @AuthenticationPrincipal UserDetails currentUser) {
        try {
            User user = userService.getUser(currentUser);
            WorkoutPlan plan = planService.getOrCreateDefaultPlan(user);

            List<PlanExercise> planExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
            WorkoutPlanDto workoutPlanDto = new WorkoutPlanDto();
            workoutPlanDto.setDaysPerWeek(plan.getDaysPerWeek());
            workoutPlanDto.setPlanExerciseList(new ArrayList<>());
            planExercises.forEach(pe -> workoutPlanDto.getPlanExerciseList().add(new PlanExerciseDto(pe)));

            httpSession.setAttribute("workoutPlanDto", workoutPlanDto);
            model.addAttribute("workoutPlanDto", workoutPlanDto);
            model.addAttribute("currentDay", null);
            model.addAttribute("trackingParameters", TrackingParameter.values());
        } catch (Exception e){
            model.addAttribute("message", e.getMessage());
            return "error";
        }

        return "plan_creator";
    }

    @GetMapping("/plan-creator/{day}")
    public String planCreatorForDay(@PathVariable("day") int day, HttpSession httpSession, Model model) {
        WorkoutPlanDto workoutPlanDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");
        if (workoutPlanDto == null) {
            return "redirect:/plan";
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
            @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
            HttpSession httpSession)
    {
        if(currentDay != 8) planService.updateSessionFromForm(currentDay, updatedPlanDto, httpSession);
        return "redirect:/plan/plan-creator/" + targetDay;
    }

    @PostMapping("/plan-creator/{day}/add-exercise")
    public String addExerciseToDay(@PathVariable("day") Integer day,
                                   @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                                   HttpSession httpSession) {
        try {
            WorkoutPlanDto workoutPlanDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

            planService.updateSessionFromForm(day, updatedPlanDto, httpSession);

            if (workoutPlanDto != null) {
                planService.ensureExerciseList(workoutPlanDto);
                long exerciseCount = workoutPlanDto.getPlanExerciseList().stream()
                        .filter(e -> e.getDayNumber() == day)
                        .count();

                workoutPlanDto.getPlanExerciseList().add(
                        new PlanExerciseDto("", 3, "REPETITIONS", "", day, (int) (exerciseCount + 1))
                );

                httpSession.setAttribute("workoutPlanDto", workoutPlanDto);
            }
        } catch (Exception e){
            return "error";
        }
        return "redirect:/plan/plan-creator/" + day;
    }

    @PostMapping(value = {"/plan-creator/change-days", "/plan-creator/{day}/change-days"})
    public String changeDays(@PathVariable(value = "day", required = false) Integer day,
                             @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                             HttpSession httpSession,
                             @AuthenticationPrincipal UserDetails currentUser) {

        try {
            User user = userService.getUser(currentUser);
            WorkoutPlan plan = planService.getWorkoutPlan(user);

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
                                        @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                                        HttpSession httpSession) {
        planService.updateSessionFromForm(day, updatedPlanDto, httpSession);

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

    @PostMapping("/plan-creator/move-up/{day}/{exercise}")
    public String moveUpExercise(@PathVariable("day") Integer day,
                                 @PathVariable("exercise") Integer exercise,
                                 @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                                 HttpSession httpSession)
    {
        updatedPlanDto.getPlanExerciseList().removeIf(exerciseDto ->exerciseDto.getTrackingParameter() == null);
        System.out.println("Exercise number to switch: " + exercise);

        if(day < 1 || day > 7) throw new RuntimeException("Wystąpił błąd, podano nieprawidłowy parametr: dzień");
        if(exercise <= 1) return "redirect:/plan/plan-creator/" + day;

        PlanExerciseDto firstExercise = updatedPlanDto.getPlanExerciseList().stream().
                filter(ex -> day.equals(ex.getDayNumber()) && ex.getExerciseNumber() == exercise).findFirst().
                orElseThrow(() -> new RuntimeException("Nie znaleziono ćwiczenia z takim numerem: " + exercise + " z dnia: " + day));

        PlanExerciseDto secondExercise = updatedPlanDto.getPlanExerciseList().stream().
                filter(ex -> day.equals(ex.getDayNumber()) && ex.getExerciseNumber() == exercise - 1).findFirst().
                orElseThrow(() -> new RuntimeException("Nie znaleziono ćwiczenia z takim numerem: " + (exercise - 1) + " z dnia: " + day));

        System.out.println("Setting exercise number to exercise: " + firstExercise.getExerciseName() + " Current number: " + firstExercise.getExerciseNumber() + " Changing to: " + (exercise - 1));
        System.out.println("Setting exercise number to exercise: " + secondExercise.getExerciseName() + " Current number: " + secondExercise.getExerciseNumber() + " Changing to: " + exercise);

        firstExercise.setExerciseNumber(exercise - 1);
        secondExercise.setExerciseNumber(exercise);

        updatedPlanDto.getPlanExerciseList().sort(Comparator.comparingInt(PlanExerciseDto::getExerciseNumber));

        planService.updateSessionFromForm(day, updatedPlanDto, httpSession);

        return "redirect:/plan/plan-creator/" + day;
    }

    @PostMapping("/plan-creator/move-down/{day}/{exercise}")
    public String moveDownExercise(@PathVariable("day") Integer day,
                                   @PathVariable("exercise") Integer exercise,
                                   @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                                   HttpSession httpSession)
    {
        updatedPlanDto.getPlanExerciseList().removeIf(exerciseDto ->exerciseDto.getTrackingParameter() == null);

        updatedPlanDto.getPlanExerciseList().removeIf(Objects::isNull);

        System.out.println("Exercise number to switch: " + exercise);
        if(day < 1 || day > 7) throw new RuntimeException("Wystąpił błąd, podano nieprawidłowy parametr: dzień");
        int exercisesCount = updatedPlanDto.getPlanExerciseList().size();

        if(exercise >= exercisesCount) return "redirect:/plan/plan-creator/" + day;

        PlanExerciseDto firstExercise = updatedPlanDto.getPlanExerciseList().stream().
                filter(ex -> day.equals(ex.getDayNumber()) && ex.getExerciseNumber() == exercise).findFirst().
                orElseThrow(() -> new RuntimeException("Nie znaleziono ćwiczenia z takim numerem: " + exercise + " z dnia: " + day));

        PlanExerciseDto secondExercise = updatedPlanDto.getPlanExerciseList().stream().
                filter(ex -> day.equals(ex.getDayNumber()) && ex.getExerciseNumber() == exercise + 1).findFirst().
                orElseThrow(() -> new RuntimeException("Nie znaleziono ćwiczenia z takim numerem: " + (exercise + 1) + " z dnia: " + day));

        firstExercise.setExerciseNumber(exercise + 1);
        secondExercise.setExerciseNumber(exercise);

        updatedPlanDto.getPlanExerciseList().sort(Comparator.comparingInt(PlanExerciseDto::getExerciseNumber));

        planService.updateSessionFromForm(day, updatedPlanDto, httpSession);

        return "redirect:/plan/plan-creator/" + day;
    }

    @PostMapping(value = {"/plan-creator/save", "/plan-creator/{day}/save"})
    public String savePlan(@PathVariable(value = "day", required = false) Integer day,
                           @ModelAttribute("workoutPlanDto") WorkoutPlanDto updatedPlanDto,
                           HttpSession httpSession,
                           @AuthenticationPrincipal UserDetails currentUser) {

        try {
            if (day != null) {
                planService.updateSessionFromForm(day, updatedPlanDto, httpSession);
            }

            User user = userService.getUser(currentUser);
            WorkoutPlan plan = planService.getWorkoutPlan(user);

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

            return "redirect:/plan";
        } catch (Exception e){
            e.printStackTrace();
            return "error";
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
