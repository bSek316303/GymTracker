package org.key0.gymtracker.controllers;

import lombok.AllArgsConstructor;
import org.key0.gymtracker.models.Training;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.ExerciseResultRepository;
import org.key0.gymtracker.repositories.TrainingRepository;
import org.key0.gymtracker.repositories.UserRepository;
import org.key0.gymtracker.repositories.WorkoutPlanRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/training")
@AllArgsConstructor
public class TrainingController {
    private final TrainingRepository trainingRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final UserRepository userRepository;

    @GetMapping("/choose-training")
    public String chooseTraining(@AuthenticationPrincipal UserDetails userDetails, Model model){
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika: " + userDetails.getUsername()));

        Optional<WorkoutPlan> userPlan = workoutPlanRepository.findByUserId(user.getId());

        if(userPlan.isEmpty()){
            model.addAttribute("emptyPlan", true);
            return "choose-training";
        }
        model.addAttribute("emptyPlan", false);
        model.addAttribute("daysInPlan", userPlan.get().getDaysPerWeek());

        List<Training> userTrainingSessions = trainingRepository.findByPlanOrderByTrainingDateDesc(userPlan.get());

        if(userTrainingSessions.isEmpty()){
            model.addAttribute("weeksCount", 1);
            model.addAttribute("sessions", List.of());
        } else {
            model.addAttribute("weeksCoumt", userTrainingSessions.get(0).getTrainingWeek());
            model.addAttribute("sessions", userTrainingSessions);
        }

        return "choose_training";
    }
}
