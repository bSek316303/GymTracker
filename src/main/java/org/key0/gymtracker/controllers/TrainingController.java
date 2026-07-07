package org.key0.gymtracker.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.ExerciseResultDto;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.Training;
import org.key0.gymtracker.models.User;
import org.key0.gymtracker.models.WorkoutPlan;
import org.key0.gymtracker.repositories.*;
import org.key0.gymtracker.services.UserService;
import org.key0.gymtracker.services.WorkoutService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/training")
@AllArgsConstructor
public class TrainingController {
    private final TrainingRepository trainingRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final WorkoutService workoutService;

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
            model.addAttribute("weeksCount", userTrainingSessions.getFirst().getTrainingWeek());
            model.addAttribute("sessions", userTrainingSessions);
        }

        return "choose_training";
    }

    @GetMapping("/{week-number}/{day-number}/{exercise-number}")
    public String trainingSession(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable("week-number") Integer weekNumber,
                                  @PathVariable("day-number") Integer dayNumber,
                                  @PathVariable("exercise-number") Integer exerciseNumber,
                                  Model model, HttpSession httpSession  ) {
        try{
            User user = userService.getUser(userDetails);
            WorkoutPlan workoutPlan = workoutService.getWorkoutPlan(user);
            Optional<Training> oTraining = trainingRepository.findByTrainingWeekAndDayNumber(weekNumber, dayNumber);

            if(oTraining.isEmpty()){
                Training newTraining = new Training();
                newTraining.setTrainingDate(java.time.LocalDate.now());
                newTraining.setTrainingWeek(weekNumber);
                newTraining.setDayNumber(dayNumber);
                newTraining.setPlan(workoutPlan);
                trainingRepository.save(newTraining);
                oTraining = Optional.of(newTraining);
            }

            List<PlanExercise> exercisesFromPlan = planExerciseRepository.findByPlanIdAndDayNumberOrderByExerciseNumberAsc(workoutPlan.getId(), dayNumber);

            final Long trainingId = oTraining.get().getId();

            List<ExerciseResultDto> exerciseResultDtos = (List<ExerciseResultDto>) httpSession.getAttribute("exerciseResultDtos");

            if (exerciseResultDtos == null) {
                exerciseResultDtos = new ArrayList<>();
                var finalExerciseResultDtos = exerciseResultDtos;
                exercisesFromPlan.forEach(planExercise ->
                        finalExerciseResultDtos.add(new ExerciseResultDto(trainingId, planExercise.getTargetSets(), planExercise.getTrackingParameter()))
                );
                httpSession.setAttribute("exerciseResultDtos", finalExerciseResultDtos);
            }

            PlanExercise currentPlanExercise = exercisesFromPlan.stream()
                    .filter(ex -> ex.getExerciseNumber() == exerciseNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono ćwiczenia o numerze: " + exerciseNumber));

            int exerciseIndex = exercisesFromPlan.indexOf(currentPlanExercise);
            ExerciseResultDto currentExerciseResultDto = exerciseResultDtos.get(exerciseIndex);

            boolean isLastExercise = (exerciseNumber == exercisesFromPlan.size());

            model.addAttribute("planExercise", currentPlanExercise);
            model.addAttribute("currentExerciseResult", currentExerciseResultDto);
            model.addAttribute("isLastExercise", isLastExercise);
            model.addAttribute("currentExerciseNumber", exerciseNumber);
            model.addAttribute("weekNumber", weekNumber);
            model.addAttribute("dayNumber", dayNumber);

            return "training";
        } catch(RuntimeException e){
            model.addAttribute("error", e.getMessage());
        }
        return "error";
    }
}
