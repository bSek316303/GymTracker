package org.key0.gymtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.PlanExercise;

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

    public PlanExercise toPlanExerciseWithoutPlan(){
        TrackingParameter trackingParameterEnum = TrackingParameter.fromString(trackingParameter);
        PlanExercise planExercise = new PlanExercise();
        planExercise.setExerciseName(exerciseName);
        planExercise.setExerciseNumber(exerciseNumber);
        planExercise.setDayNumber(dayNumber);
        planExercise.setTrackingParameter(trackingParameterEnum);
        planExercise.setNotes(notes);
        planExercise.setTargetSets(targetSets);
        return planExercise;
    }
};

