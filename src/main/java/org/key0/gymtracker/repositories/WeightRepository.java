package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.Weight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeightRepository extends JpaRepository<Weight, Long> {
    List<Weight> findByUserIdOrderByWeightAsc(Long userId);
}
