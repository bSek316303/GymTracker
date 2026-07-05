package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.Training;
import org.key0.gymtracker.models.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingRepository extends JpaRepository<Training, Long> {
    List<Training> findByPlanOrderByTrainingDateDesc(WorkoutPlan plan);
}
