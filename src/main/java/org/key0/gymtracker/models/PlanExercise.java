package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Data;

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
    private String targetSets;

    @Column(name="notes", nullable = true)
    private String notes;

    @Column(name="day_number", nullable = false)
    private int dayNumber;

    @Column(name="exercise_number", nullable = false)
    private int exerciseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;
}
