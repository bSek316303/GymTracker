package org.key0.gymtracker.services;

import lombok.AllArgsConstructor;
import org.key0.gymtracker.models.Training;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.TrainingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class TrainingService {
    private final PlanService planService;
    private final TrainingRepository trainingRepository;

    public boolean isUsersTrainingWeekFinished(User user, Integer trainingWeek){
        WorkoutPlan plan = planService.getWorkoutPlan(user);
        List<Training> userSessions = trainingRepository.findByPlanOrderByTrainingDateDesc(plan);

        if (userSessions == null || userSessions.isEmpty()) return false;
        if(userSessions.getFirst().getTrainingWeek() > trainingWeek) return true;

        return userSessions.stream().filter(training -> trainingWeek.equals(training.getTrainingWeek())).count() == plan.getDaysPerWeek();
    }
}
