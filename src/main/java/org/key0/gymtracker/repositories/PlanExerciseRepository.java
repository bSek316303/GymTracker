package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.Weight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanExerciseRepository extends JpaRepository<PlanExercise, Long> {
    List<PlanExercise> findByPlanIdOrderByExerciseNumberAsc(Long planId);
}
