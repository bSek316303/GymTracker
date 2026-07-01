package org.key0.gymtracker.services;

import jakarta.transaction.Transactional;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.Weight;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WeightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class WeightService {
    private final WeightRepository weightRepository;
    private final UserRepository userRepository;

    public WeightService(WeightRepository weightRepository, UserRepository userRepository) {
        this.weightRepository = weightRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void addWeightToUser(String username, Double weightValue) {
        if (weightValue == null || weightValue < 0.0) {
            throw new IllegalArgumentException("Waga nie może być ujemna ani pusta");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika"));

        Weight weight = new Weight();
        weight.setUser(user);
        weight.setWeight(weightValue);
        weight.setWeightDate(LocalDate.now());

        weightRepository.save(weight);
    }
}
