package org.key0.gymtracker.dto;

import java.io.Serializable;

public record PlanExerciseDto(
        String exerciseName,
        String targetSets,
        String notes,
        int dayNumber,
        int exerciseNumber
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
