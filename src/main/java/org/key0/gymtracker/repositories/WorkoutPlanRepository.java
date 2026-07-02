package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.models.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    Optional<WorkoutPlan> findByUserId(Long id);
}
