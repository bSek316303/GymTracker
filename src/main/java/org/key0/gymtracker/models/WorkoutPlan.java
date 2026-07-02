package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Data;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
