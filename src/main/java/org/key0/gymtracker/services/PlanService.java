package org.key0.gymtracker.services;

import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.WorkoutPlanDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@AllArgsConstructor
public class PlanService {
    public void shrinkPlanIfNeeded(WorkoutPlanDto planDto){
        ensureExerciseList(planDto);

        int totalDays = planDto.getDaysPerWeek();
        for (int currentDayCheck = 1; currentDayCheck <= totalDays; currentDayCheck++) {

            final int emptyDayCandidate = currentDayCheck;

            boolean hasExercises = planDto.getPlanExerciseList().stream()
                    .anyMatch(pe -> pe.getDayNumber() == emptyDayCandidate);

            if (!hasExercises) {

                planDto.getPlanExerciseList().stream()
                        .filter(pe -> pe.getDayNumber() > emptyDayCandidate)
                        .forEach(pe -> pe.setDayNumber(pe.getDayNumber() - 1));

                totalDays--;
                planDto.setDaysPerWeek(totalDays);

                currentDayCheck--;
            }
        }
    }

    public void ensureExerciseList(WorkoutPlanDto workoutPlanDto) {
        if (workoutPlanDto.getPlanExerciseList() == null) {
            workoutPlanDto.setPlanExerciseList(new ArrayList<>());
        }
    }
}
