package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name="workout_plans")
@Data
public class WorkoutPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "days_per_week", nullable = false)
    private int daysPerWeek;

    @Column(name = "last_edit_date", nullable = false)
    private LocalDate lastEditDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
