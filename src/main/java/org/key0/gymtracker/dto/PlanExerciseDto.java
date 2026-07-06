package org.key0.gymtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanExerciseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String exerciseName;
    private int targetSets;
    private String trackingParameter;
    private String notes;
    private int dayNumber;
    private int exerciseNumber;
};

