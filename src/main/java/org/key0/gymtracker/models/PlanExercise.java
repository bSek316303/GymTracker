package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.enums.TrackingParameter;

@Entity
@Table(name="plan_exercises")
@Data
public class PlanExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="exercise_name", nullable = false)
    private String exerciseName;

    @Column(name="target_sets", nullable = false)
    private int targetSets;

    @Enumerated(EnumType.STRING)
    @Column(name="tracking_parameter", nullable = false)
    private TrackingParameter trackingParameter;

    @Column(name="guidelines", nullable = true)
    private String guidelines;

    @Column(name="day_number", nullable = false)
    private int dayNumber;

    @Column(name="exercise_number", nullable = false)
    private int exerciseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;

    public void updateFromPlanExerciseDto(PlanExerciseDto planExerciseDto){
        this.exerciseName = planExerciseDto.getExerciseName();
        this.dayNumber = planExerciseDto.getDayNumber();
        this.exerciseNumber = planExerciseDto.getExerciseNumber();
        this.guidelines = planExerciseDto.getGuidelines();
        this.targetSets = planExerciseDto.getTargetSets();
        this.trackingParameter = TrackingParameter.fromString(planExerciseDto.getTrackingParameter());
    }
}
