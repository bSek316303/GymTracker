package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name="trainings")
@Data
public class Training {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="day_number", nullable = false)
    private Integer dayNumber;

    @Column(name="training_date", nullable = false)
    private LocalDate trainingDate;

    @Column(name="training_week", nullable = true)
    private Integer trainingWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;
}
