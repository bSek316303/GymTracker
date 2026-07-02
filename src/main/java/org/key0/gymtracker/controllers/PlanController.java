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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String planCreator(HttpSession httpSession, @AuthenticationPrincipal UserDetails currentUser) {
        User user = userRepository.findByUsername(currentUser.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + currentUser.getUsername()));

        Optional<WorkoutPlan> oWorkoutPlan = workoutPlanRepository.findByUserId(user.getId());

        if (oWorkoutPlan.isPresent()) {
            WorkoutPlan existingPlan = oWorkoutPlan.get();
            List<PlanExercise> planExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(existingPlan.getId());

            WorkoutPlanDto workoutPlanDto = new WorkoutPlanDto();
            workoutPlanDto.setDaysPerWeek(existingPlan.getDaysPerWeek());
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
        } else {
            WorkoutPlanDto workoutPlanDto = new WorkoutPlanDto();
            workoutPlanDto.setDaysPerWeek(3);
            workoutPlanDto.setPlanExerciseList(new ArrayList<>());
            workoutPlanDto.getPlanExerciseList().add(new PlanExerciseDto("", 3, "", 1, 1));

            httpSession.setAttribute("workoutPlanDto", workoutPlanDto);
        }

        return "plan_creator";
    }

    @GetMapping("/plan-creator/{day}")
    public String planCreatorForDay(@PathVariable("day") int day, HttpSession httpSession, Model model){
        WorkoutPlanDto workoutPlanDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        List<PlanExerciseDto> exercisesForCurrentDay = workoutPlanDto.getPlanExerciseList().stream()
                .filter(ex -> ex.getDayNumber() == day)
                .collect(Collectors.toList());

        model.addAttribute("currentDay", day);
        model.addAttribute("exercises", exercisesForCurrentDay);

        return "plan_creator";
    }
}
