package org.key0.gymtracker.services;

import lombok.AllArgsConstructor;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class WorkoutService {
    private final WorkoutPlanRepository workoutPlanRepository;
    private final TrainingRepository trainingRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final SetLogRepository setLogRepository;

    public WorkoutPlan getWorkoutPlan(User user){
        WorkoutPlan workoutPlan = workoutPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono planu treningowego dla użytkownika: " + user.getUsername()));
        return workoutPlan;
    }

    public Training getTrainingByWeekAndWorkout(WorkoutPlan plan, Integer week, Integer day){
        List<Training> trainingList = trainingRepository.findByPlanOrderByTrainingDateDesc(plan);
        Optional<Training> training = trainingList.stream()
                .filter(t -> t.getTrainingWeek().equals(week) && t.getDayNumber().equals(day))
                .findFirst();
        return training.orElseThrow(() -> new RuntimeException("Nie znaleziono treningu dla planu: " + plan.getId() + " w tygodniu: " + week));
    }

    public List<SetLog> getSetLogsByWorkoutAndWeek(WorkoutPlan plan, Integer week, Integer day, PlanExercise currentPlanExercise) {
        try {
            Training lastWeekTraining = getTrainingByWeekAndWorkout(plan, week, day);
            if (lastWeekTraining == null) {
                return List.of();
            }

            List<ExerciseResult> lastWeekResults = exerciseResultRepository.findByTraining(lastWeekTraining);

            Optional<ExerciseResult> lastWeekResultOpt = lastWeekResults.stream()
                    .filter(er -> er.getExercise() != null && currentPlanExercise != null
                            && er.getExercise().getId().equals(currentPlanExercise.getId()))
                    .findFirst();

            if (lastWeekResultOpt.isEmpty()) {
                return List.of();
            }

            return setLogRepository.findByExerciseResultOrderBySetNumberAsc(lastWeekResultOpt.get());

        } catch (Exception e) {
            return List.of();
        }
    }
}
