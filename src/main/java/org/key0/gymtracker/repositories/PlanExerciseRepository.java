package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.Weight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanExerciseRepository extends JpaRepository<PlanExercise, Long> {
    List<PlanExercise> findByPlanIdOrderByExerciseNumberAsc(Long planId);
    List<PlanExercise> findByPlanIdAndDayNumberOrderByDayNumberAsc(Long planId, int dayNumber);
    Optional<PlanExercise> findByPlanIdAndDayNumberAndExerciseNumber(Long planId, Integer dayNumber, Integer exerciseNumber);
}
