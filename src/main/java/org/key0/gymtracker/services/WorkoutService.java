package org.key0.gymtracker.services;

import lombok.AllArgsConstructor;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.WorkoutPlanRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class WorkoutService {
    private final WorkoutPlanRepository workoutPlanRepository;

    public WorkoutPlan getWorkoutPlan(User user){
        WorkoutPlan workoutPlan = workoutPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono planu treningowego dla użytkownika: " + user.getUsername()));
        return workoutPlan;
    }
}
