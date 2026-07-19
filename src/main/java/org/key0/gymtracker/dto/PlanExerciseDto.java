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
    private String guidelines;
    private int dayNumber;
    private int exerciseNumber;

    public PlanExerciseDto (PlanExercise pe){
        this.exerciseName = pe.getExerciseName();
        this.targetSets = pe.getTargetSets();
        this.trackingParameter = pe.getTrackingParameter().name();
        this.guidelines = pe.getGuidelines();
        this.dayNumber = pe.getDayNumber();
        this.exerciseNumber = pe.getExerciseNumber();
    }

    public PlanExercise toPlanExerciseWithoutPlan(){
        TrackingParameter trackingParameterEnum = TrackingParameter.fromString(trackingParameter);
        PlanExercise planExercise = new PlanExercise();
        planExercise.setExerciseName(exerciseName);
        planExercise.setExerciseNumber(exerciseNumber);
        planExercise.setDayNumber(dayNumber);
        planExercise.setTrackingParameter(trackingParameterEnum);
        planExercise.setGuidelines(guidelines);
        planExercise.setTargetSets(targetSets);
        return planExercise;
    }
};

