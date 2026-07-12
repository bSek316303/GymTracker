package org.key0.gymtracker.services;

import org.key0.gymtracker.models.User;
import org.key0.gymtracker.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Spring automatycznie wstrzyknie repozytorium oraz BCrypt z SecurityConfig
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean checkUsernameAvailability(User user){
        return userRepository.findByUsername(user.getUsername()).isPresent();
    }

    public boolean checkEmailAvailability(User user){
        return userRepository.findByEmail(user.getEmail()).isPresent();
    }

    public void registerNewUser(User user) {
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        userRepository.save(user);
    }

    public User getUser(UserDetails userDetails){
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + userDetails.getUsername()));
    }
}
