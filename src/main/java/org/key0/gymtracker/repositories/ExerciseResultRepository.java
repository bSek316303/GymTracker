package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.ExerciseResult;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.Training;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, Long> {
    List<ExerciseResult> findByTraining(Training training);
    Optional<ExerciseResult> findByTrainingAndExercise(Training training, PlanExercise exercise);
}
