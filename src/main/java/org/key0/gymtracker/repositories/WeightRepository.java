package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.Weight;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WeightRepository extends JpaRepository<Weight, Long> {
    List<Weight> findByUserId(Long userId);
}
