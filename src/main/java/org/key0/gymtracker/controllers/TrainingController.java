package org.key0.gymtracker.controllers;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.ExerciseResultDto;
import org.key0.gymtracker.dto.SetLogDto;
import org.key0.gymtracker.enums.TrackingParameter;
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

@Controller
@RequestMapping("/training")
@AllArgsConstructor
public class TrainingController {
    private final TrainingRepository trainingRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final SetLogRepository setLogRepository;
    private final UserService userService;
    private final PlanService planService;
    private final TrainingService trainingService;

    @GetMapping("/choose-training")
    public String chooseTraining(@AuthenticationPrincipal UserDetails currentUser, Model model){
        User user = userService.getUser(currentUser);

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
        try{
            User user = userService.getUser(currentUser);
            WorkoutPlan plan = planService.getWorkoutPlan(user);

            Optional<Training> currentTraining = trainingRepository.findByTrainingWeekAndDayNumberAndPlan(weekNumber, dayNumber, plan);
            Optional<PlanExercise> currentExercise = planExerciseRepository.findByPlanIdAndDayNumberAndExerciseNumber(plan.getId(), dayNumber, oldExerciseNumber);
            Optional<ExerciseResult> currentExerciseResult = exerciseResultRepository.findByTrainingAndExercise(currentTraining.get(), currentExercise.get());

            if(currentExerciseResult.isEmpty()){
                ExerciseResult newExerciseResult = new ExerciseResult();
                newExerciseResult.setExercise(currentExercise.get());
                newExerciseResult.setTraining(currentTraining.get());
                exerciseResultRepository.save(newExerciseResult);

                currentExerciseResult = Optional.of(newExerciseResult);
            }

            final ExerciseResult finalExerciseResult = currentExerciseResult.get();

            List<SetLog> setLogList = setLogRepository.findByExerciseResultOrderBySetNumberAsc(finalExerciseResult);

            if(setLogList.isEmpty()) {
                setLogList = currentExerciseDto.getSetLogs().stream().map(setLogDto -> {
                    SetLog setLog = setLogDto.toSetLogWithoutParameter();
                    setLog.setValueByParameter(setLogDto.getParameter(), finalExerciseResult.getExercise().getTrackingParameter());
                    setLog.setExerciseResult(finalExerciseResult);
                    return setLog;
                }).toList();
            } else {
                setLogList = setLogList.stream().map(setLog -> {
                    Optional<SetLogDto> setLogDto = currentExerciseDto.getSetLogs().stream().filter(setLogDtoArg -> Objects.equals(setLogDtoArg.getSetNumber(), setLog.getSetNumber())).findFirst();
                    if(!setLogDto.get().isEmpty()){
                        setLog.updateFromSetLogDto(setLogDto.get());
                    }
                    return setLog;
                }).toList();
            }
            setLogRepository.saveAll(setLogList);
            httpSession.setAttribute("currentExerciseDto", currentExerciseDto);

        } catch(Exception e){
            return "error";
        }

        return "redirect:/" + weekNumber + "/" + dayNumber + "/" + newExerciseNumber;
    }

    @GetMapping("/{week-number}/{day-number}/{exercise-number}")
    public String getExerciseInTraining(@AuthenticationPrincipal UserDetails userDetails,
                                  @PathVariable("week-number") Integer weekNumber,
                                  @PathVariable("day-number") Integer dayNumber,
                                  @PathVariable("exercise-number") Integer exerciseNumber,
                                  Model model, HttpSession httpSession  ) {
        try {
            Map<Integer, ExerciseResultDto> exerciseResultDtoMap = (Map<Integer, ExerciseResultDto>)httpSession.getAttribute("exerciseResultDtoMap");
            List<PlanExercise> planExerciseList = exerciseResultDtoMap.values().stream().map(ExerciseResultDto::getExercise).toList();
            Map<Integer, ExerciseResult> historyExerciseResults = (Map<Integer, ExerciseResult>)httpSession.getAttribute("historyExerciseResultsMap");
            ExerciseResult historyExerciseResult = historyExerciseResults.get(exerciseNumber);

            boolean isLastExercise = !exerciseResultDtoMap.containsKey(2);

            // Informacje dotyczące liczb
            model.addAttribute("weekNumber", weekNumber);
            model.addAttribute("dayNumber", dayNumber);
            model.addAttribute("currentExerciseNumber", exerciseNumber);
            model.addAttribute("isLastExercise", isLastExercise);
            model.addAttribute("parameter", exerciseResultDtoMap.get(exerciseNumber).getExercise().getTrackingParameter().toString());

            // Informacje dotyczące złożonych obiektów
            model.addAttribute("currentExerciseDto", exerciseResultDtoMap.get(exerciseNumber));
            model.addAttribute("planExerciseList", planExerciseList);
            model.addAttribute("historyExerciseResult", historyExerciseResult);


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

            Map<Integer, ExerciseResult> historyExerciseResults = null;
            if(weekNumber > 1) {
                Training lastWeekTraining = trainingService.getTraining(weekNumber - 1, dayNumber, workoutPlan);
                historyExerciseResults = trainingService.getExerciseResultMap(lastWeekTraining);
            }

            httpSession.setAttribute("exerciseResultDtoMap", exerciseResultDtoMap);
            httpSession.setAttribute("historyExerciseResultsMap", historyExerciseResults);

            return "redirect:/training/" + weekNumber + "/" + dayNumber + "/" + targetExerciseNumber;

        } catch(Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        return "error";
    }
}
