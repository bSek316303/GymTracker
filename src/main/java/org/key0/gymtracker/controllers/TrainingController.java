package org.key0.gymtracker.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.ExerciseResultDto;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.*;
import org.key0.gymtracker.services.TrainingService;
import org.key0.gymtracker.services.UserService;
import org.key0.gymtracker.services.PlanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/training")
@AllArgsConstructor
public class TrainingController {
    private final TrainingRepository trainingRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final SetLogRepository setLogRepository;
    private final UserService userService;
    private final PlanService planService;
    private final TrainingService trainingService;
    private final PlanExerciseRepository planExerciseRepository;

    @GetMapping("/choose-training")
    public String chooseTraining(@AuthenticationPrincipal UserDetails currentUser, Model model){
        User user = userService.getUser(currentUser);

        Optional<WorkoutPlan> userPlan = workoutPlanRepository.findByUserId(user.getId());

        if(userPlan.isEmpty()){
            model.addAttribute("emptyPlan", true);
            return "redirect:/plan";
        }

        model.addAttribute("emptyPlan", false);
        model.addAttribute("daysInPlan", userPlan.get().getDaysPerWeek());

        List<Training> userTrainingSessions = trainingRepository.findByPlanOrderByTrainingDateDesc(userPlan.get());

        if(userTrainingSessions.isEmpty()){
            model.addAttribute("weeksCount", 1);
            model.addAttribute("sessions", List.of());
        } else {
            if(trainingService.isUsersTrainingWeekFinished(user, userTrainingSessions.getFirst().getTrainingWeek()))
                model.addAttribute("weeksCount", userTrainingSessions.getFirst().getTrainingWeek() + 1);
            else
                model.addAttribute("weeksCount", userTrainingSessions.getFirst().getTrainingWeek());

            model.addAttribute("sessions", userTrainingSessions);
        }

        return "choose_training";
    }
    @PostMapping("/{week-number}/{day-number}/{old-exercise-number}/{new-exercise-number}")
    public String changeExercise(@AuthenticationPrincipal UserDetails currentUser,
                                 @ModelAttribute("currentExerciseDto") ExerciseResultDto currentExerciseDto,
                                 @PathVariable("week-number") Integer weekNumber,
                                 @PathVariable("day-number") Integer dayNumber,
                                 @PathVariable("old-exercise-number") Integer oldExerciseNumber,
                                 @PathVariable("new-exercise-number") Integer newExerciseNumber,
                                 Model model, HttpSession httpSession  ) {
        try
        {
            trainingService.saveExerciseResultDto(currentExerciseDto);

            Map<Long, PlanExercise> planExerciseMap = (Map<Long, PlanExercise>)httpSession.getAttribute("planExerciseMap");

            if(planExerciseMap.values().stream().noneMatch(pe -> pe.getExerciseNumber() == newExerciseNumber)){
                return "redirect:/profile";
                // Tutaj w przyszłości można wrzucić podsumowanie sesji treningowej póki co jest tak.
            }

            Map<Integer, ExerciseResultDto> map = (Map<Integer, ExerciseResultDto>) httpSession.getAttribute("exerciseResultDtoMap");
            map.put(oldExerciseNumber, currentExerciseDto);
            httpSession.setAttribute("exerciseResultDtoMap", map);
        } catch(Exception e){
            model.addAttribute("message", e.getMessage());
            return "error";
        }

        return "redirect:/training/" + weekNumber + "/" + dayNumber + "/" + newExerciseNumber;
    }

    @GetMapping("/{week-number}/{day-number}/{exercise-number}")
    public String getExerciseInTraining(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable("week-number") Integer weekNumber,
                                  @PathVariable("day-number") Integer dayNumber,
                                  @PathVariable("exercise-number") Integer exerciseNumber,
                                  Model model, HttpSession httpSession  ) {
        try {
            Map<Integer, ExerciseResultDto> exerciseResultDtoMap = (Map<Integer, ExerciseResultDto>)httpSession.getAttribute("exerciseResultDtoMap");
            Map<Long, PlanExercise> planExerciseMap = (Map<Long, PlanExercise>)httpSession.getAttribute("planExerciseMap");
            List<PlanExercise> planExerciseList = planExerciseMap.values().stream().toList();
            Map<Integer, ExerciseResult> historyExerciseResults = null;
            ExerciseResult historyExerciseResult = null;
            List<SetLog> historySetLogs = null;

            if(weekNumber > 1) {
                historyExerciseResults = (Map<Integer, ExerciseResult>)httpSession.getAttribute("historyExerciseResultsMap");
                historyExerciseResult = historyExerciseResults.get(exerciseNumber);
                historySetLogs = setLogRepository.findByExerciseResultId(historyExerciseResult.getId());
            }

            boolean isLastExercise = !exerciseResultDtoMap.containsKey(exerciseNumber + 1);

            // Informacje dotyczące prostych parametrów
            model.addAttribute("weekNumber", weekNumber);
            model.addAttribute("dayNumber", dayNumber);
            model.addAttribute("currentExerciseNumber", exerciseNumber);
            model.addAttribute("isLastExercise", isLastExercise);
            model.addAttribute("parameter", planExerciseMap.get(exerciseResultDtoMap.get(exerciseNumber).getExerciseId()).getTrackingParameter().toString());

            // Informacje dotyczące złożonych obiektów
            model.addAttribute("currentExerciseDto", exerciseResultDtoMap.get(exerciseNumber));
            model.addAttribute("planExerciseList", planExerciseList);
            model.addAttribute("historyExerciseResult", historyExerciseResult);
            model.addAttribute("historySetLogs", historySetLogs);


        } catch(Exception e){
            return "error";
        }
        return "training";
    }

    @PostMapping("/{week-number}/{day-number}/{target-exercise-number}")
    @Transactional
    public String prepareTraining(@AuthenticationPrincipal UserDetails userDetails,
                                     @PathVariable("week-number") Integer weekNumber,
                                     @PathVariable("day-number") Integer dayNumber,
                                     @PathVariable("target-exercise-number") Integer targetExerciseNumber,
                                     HttpSession httpSession, Model model) {
        try {
            User user = userService.getUser(userDetails);
            WorkoutPlan workoutPlan = planService.getWorkoutPlan(user);
            Training training = trainingService.getOrCreateTraining(weekNumber, dayNumber, workoutPlan); // Możemy albo rozpocząć nowy trening albo wrócić do starego.
            trainingService.prepareTraining(training);
            Map<Integer, ExerciseResultDto> exerciseResultDtoMap = trainingService.getExerciseResultDtoMap(training);
            Map<Long, PlanExercise> planExerciseMap = planExerciseRepository.findByPlanIdAndDayNumberOrderByExerciseNumberAsc(workoutPlan.getId(), dayNumber)
                    .stream().collect(Collectors.toMap(planExercise->planExercise.getId(), planExercise -> planExercise));

            Map<Integer, ExerciseResult> historyExerciseResults = null;
            if(weekNumber > 1) {
                Training lastWeekTraining = trainingService.getTraining(weekNumber - 1, dayNumber, workoutPlan);
                historyExerciseResults = trainingService.getExerciseResultMap(lastWeekTraining);
            }

            httpSession.setAttribute("exerciseResultDtoMap", exerciseResultDtoMap);
            httpSession.setAttribute("historyExerciseResultsMap", historyExerciseResults);
            httpSession.setAttribute("planExerciseMap", planExerciseMap);

            return "redirect:/training/" + weekNumber + "/" + dayNumber + "/" + targetExerciseNumber;

        } catch(Exception e) {
            System.err.println("BŁĄD W PREPARE TRAINING: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("error", e.getMessage());
        }
        return "error";
    }
}
