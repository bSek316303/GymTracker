package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name="set_logs")
@Data
public class SetLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="set_number", nullable = false)
    private Integer setNumber;

    @Column(name="weight", nullable = true)
    private Double weight;

    @Column(name="reps", nullable = true)
    private Integer reps;

    @Column(name="duration_seconds", nullable = true)
    private Integer durationSeconds;

    @Column(name="distance_meters", nullable = true)
    private Double distanceMeters;

    @Column(name="calories", nullable = true)
    private Integer calories;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_result_id", nullable = false)
    private ExerciseResult exerciseResult;
}
