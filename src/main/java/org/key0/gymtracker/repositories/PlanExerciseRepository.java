package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.Weight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanExerciseRepository extends JpaRepository<PlanExercise, Long> {
    List<PlanExercise> findByPlanIdOrderByExerciseNumberAsc(Long planId);
}
