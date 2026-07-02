package org.key0.gymtracker.repositories;

import org.key0.gymtracker.models.BodyMeasurement;
import org.key0.gymtracker.models.Weight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BodyMeasurementsRepository extends JpaRepository<BodyMeasurement, Long> {
    List<BodyMeasurement> findByUserId(Long userId);
}
