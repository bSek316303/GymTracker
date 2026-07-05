package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.ExerciseResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseResultRepository extends JpaRepository<ExerciseResult, Long> {
}
