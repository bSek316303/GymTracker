package org.key0.gymtracker.services;

import jakarta.transaction.Transactional;
import org.key0.gymtracker.models.BodyMeasurement;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.repositories.BodyMeasurementsRepository;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WeightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class BodyMeasurementService {
    private final BodyMeasurementsRepository bodyMeasurementsRepository;
    private final UserRepository userRepository;

    public BodyMeasurementService(BodyMeasurementsRepository bodyMeasurementsRepository, UserRepository userRepository) {
        this.bodyMeasurementsRepository = bodyMeasurementsRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addMeasurementToUser(String username, BodyMeasurement bodyMeasurement) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        if(!bodyMeasurement.validValues()) throw new IllegalArgumentException("Nieprawidłowe wartości pomiarów ciała");

        bodyMeasurement.setMeasurementDate(LocalDate.now());
        bodyMeasurement.setUser(user);

        bodyMeasurementsRepository.save(bodyMeasurement);
    }
}
