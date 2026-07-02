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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

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
        if(oWorkoutPlan.isPresent()){
            List<PlanExercise> planExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(oWorkoutPlan.get().getId());
            WorkoutPlanDto workoutPlan = new WorkoutPlanDto(planExercises.size(), List.of());
            for(int i = 0; i < planExercises.size(); i++){
                workoutPlan.planExerciseList().add(new PlanExerciseDto(planExercises.get(i).getExerciseName(), planExercises.get(i).getExerciseNumber()));
            }
        } else {


        }

        return "plan_creator";
    }
}
