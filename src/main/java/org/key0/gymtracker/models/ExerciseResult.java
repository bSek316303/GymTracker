package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="exercise_results")
@Data
public class ExerciseResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_id", nullable = false)
    private Training training;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private PlanExercise exercise;
}
