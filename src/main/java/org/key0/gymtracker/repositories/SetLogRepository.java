package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.ExerciseResult;
import org.key0.gymtracker.models.SetLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SetLogRepository extends JpaRepository<SetLog, Long> {
    public List<SetLog> findByExerciseResultOrderBySetNumberAsc(ExerciseResult exerciseResult);
    public void deleteByExerciseResult(ExerciseResult exerciseResult);
}
