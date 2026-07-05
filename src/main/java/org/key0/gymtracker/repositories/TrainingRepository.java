package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.Training;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingRepository extends JpaRepository<Training, Long> {
}
