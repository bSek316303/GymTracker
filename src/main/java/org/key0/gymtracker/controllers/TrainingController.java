package org.key0.gymtracker.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.ExerciseResultDto;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.*;
import org.key0.gymtracker.services.UserService;
import org.key0.gymtracker.services.WorkoutService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    private final SetLogRepository setLogRepository;
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
            Optional<Training> oTraining = trainingRepository.findByTrainingWeekAndDayNumberAndPlan(weekNumber, dayNumber, workoutPlan);

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
                        finalExerciseResultDtos.add(new ExerciseResultDto(trainingId, planExercise.getTargetSets(), planExercise))
                );
                httpSession.setAttribute("exerciseResultDtos", finalExerciseResultDtos);
            }

            PlanExercise currentPlanExercise = exercisesFromPlan.stream()
                    .filter(ex -> ex.getExerciseNumber() == exerciseNumber)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono ćwiczenia o numerze: " + exerciseNumber));

            int exerciseIndex = exercisesFromPlan.indexOf(currentPlanExercise);
            ExerciseResultDto currentExerciseResultDto = exerciseResultDtos.get(exerciseIndex);

            httpSession.setAttribute("lastViewedExerciseNumber", exerciseNumber);

            boolean isLastExercise = (exerciseNumber == exercisesFromPlan.size());

            if(weekNumber > 1){
                List<SetLog> lastWeekResultsInExercise = workoutService.getSetLogsByWorkoutAndWeek(workoutPlan, weekNumber - 1, dayNumber, currentPlanExercise);
                model.addAttribute("historyExerciseResults", lastWeekResultsInExercise);
            } else model.addAttribute("historyExerciseResults", null);

            model.addAttribute("parameter", currentPlanExercise.getTrackingParameter().toString());
            model.addAttribute("planExerciseList", exercisesFromPlan);
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

    @PostMapping("/{week-number}/{day-number}/{target-exercise-number}")
    @Transactional
    public String saveExerciseResult(@AuthenticationPrincipal UserDetails userDetails,
                                     @PathVariable("week-number") Integer weekNumber,
                                     @PathVariable("day-number") Integer dayNumber,
                                     @PathVariable("target-exercise-number") Integer targetExerciseNumber,
                                     HttpSession httpSession, Model model) {
        try {
            User user = userService.getUser(userDetails);
            WorkoutPlan workoutPlan = workoutService.getWorkoutPlan(user);

            Training training = trainingRepository.findByTrainingWeekAndDayNumberAndPlan(weekNumber, dayNumber, workoutPlan)
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono aktywnego treningu."));

            List<PlanExercise> exercisesFromPlan = planExerciseRepository.findByPlanIdAndDayNumberOrderByExerciseNumberAsc(workoutPlan.getId(), dayNumber);

            final Integer sourceExerciseNumber = (Integer) httpSession.getAttribute("lastViewedExerciseNumber");

            PlanExercise sourcePlanExercise = exercisesFromPlan.stream()
                    .filter(ex ->  sourceExerciseNumber.equals(ex.getExerciseNumber()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono bazowego ćwiczenia o numerze: " + sourceExerciseNumber));

            ExerciseResult exerciseResult = exerciseResultRepository.findByTrainingAndExercise(training, sourcePlanExercise)
                    .orElse(new ExerciseResult());

            exerciseResult.setTraining(training);
            exerciseResult.setExercise(sourcePlanExercise);
            ExerciseResult savedResult = exerciseResultRepository.save(exerciseResult);

            // Jeśli to była aktualizacja istniejącego ćwiczenia, czyścimy stare serie z bazy przed zapisem nowych
            if (exerciseResult.getId() != null) {
                setLogRepository.deleteByExerciseResult(savedResult);
            }

            List<ExerciseResultDto> exerciseResultDtos = (List<ExerciseResultDto>)httpSession.getAttribute("exerciseResultDtos");

            ExerciseResultDto exerciseResultDto = exerciseResultDtos.stream()
                    .filter(dto -> sourceExerciseNumber.equals(dto.getExercise().getExerciseNumber()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Nie znaleziono danych sesyjnych dla ćwiczenia o numerze: " + sourceExerciseNumber));

            List<SetLog> logsToSave = new ArrayList<>();
            exerciseResultDto.getSetLogs().forEach(setLogDto -> {
                SetLog setLog = setLogDto.toSetLogWithoutParameter();
                TrackingParameter parameter = exerciseResultDto.getExercise().getTrackingParameter();
                setLog.setValueByParameter(setLogDto.getParameter(), parameter);
                setLog.setExerciseResult(savedResult);
                logsToSave.add(setLog);
            });
            setLogRepository.saveAll(logsToSave);

            int sourceIndex = exercisesFromPlan.indexOf(sourcePlanExercise);
            exerciseResultDtos.set(sourceIndex, exerciseResultDto);
            httpSession.setAttribute("exerciseResultDtos", exerciseResultDtos);

            boolean isOutOfBounds = (targetExerciseNumber > exercisesFromPlan.size());
            boolean isLastExerciseButtonNormalClick = (sourceExerciseNumber == exercisesFromPlan.size() && targetExerciseNumber.equals(sourceExerciseNumber));
            if (isOutOfBounds || isLastExerciseButtonNormalClick) {
                httpSession.removeAttribute("exerciseResultDtos");
                httpSession.removeAttribute("lastViewedExerciseNumber");
                return "redirect:/";
            }

            return "redirect:/training/" + weekNumber + "/" + dayNumber + "/" + targetExerciseNumber;

        } catch(Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "error";
    }
}
