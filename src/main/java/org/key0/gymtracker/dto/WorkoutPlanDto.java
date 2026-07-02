package org.key0.gymtracker.dto;

import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.repositories.PlanExerciseRepository;

import java.io.Serializable;
import java.util.List;

public record WorkoutPlanDto  (
        int daysPerWeek,
        List<PlanExerciseDto> planExerciseList
) implements Serializable{
    private static final long serialVersionUID = 1L;
}
