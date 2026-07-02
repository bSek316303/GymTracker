package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.models.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {
    Optional<WorkoutPlan> findByUserId(Long id);
}
