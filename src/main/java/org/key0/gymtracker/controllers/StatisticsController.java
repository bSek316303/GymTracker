package org.key0.gymtracker.controllers;


import lombok.AllArgsConstructor;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.BodyMeasurementsRepository;
import org.key0.gymtracker.repositories.PlanExerciseRepository;
import org.key0.gymtracker.repositories.SetLogRepository;
import org.key0.gymtracker.repositories.WeightRepository;
import org.key0.gymtracker.services.PlanService;
import org.key0.gymtracker.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Controller
@RequestMapping("/statistics")
@AllArgsConstructor
public class StatisticsController {
    private final UserService userService;
    private final PlanService planService;
    private final WeightRepository weightRepository;
    private final BodyMeasurementsRepository bodyMeasurementsRepository;
    private final SetLogRepository setLogRepository;
    private final PlanExerciseRepository planExerciseRepository;

    @GetMapping("/home")
    public String statisticsHome(@AuthenticationPrincipal UserDetails currentUser){
        User user = userService.getUser(currentUser);
        WorkoutPlan plan = planService.getWorkoutPlan(user);

        return "statistics";
    }

    @GetMapping("/{statisticType}")
    public String showStatistics(@AuthenticationPrincipal UserDetails currentUser,
                                 @PathVariable("statisticType") String statisticType,
                                 @RequestParam(value = "fields", required = false) List<String> fields,
                                 @RequestParam(value = "action", required = false) String action,
                                 Model model)
    {
        if (!List.of("weight", "measurement", "exercise").contains(statisticType)) return "redirect:/statistics/weight";

        model.addAttribute("viewMode", statisticType);

        User user = userService.getUser(currentUser);

        switch (statisticType){
            case ("weight") -> {
                List<Weight> weightList = weightRepository.findByUserIdOrderByWeightDateAsc(user.getId());
                model.addAttribute("weightList", weightList);
            }
            case ("measurement") -> {
                List<BodyMeasurement> bodyMeasurementList = bodyMeasurementsRepository.findByUserIdOrderByMeasurementDateAsc(user.getId());
                // NIE PYTAĆ O TEGO POTWORA ON JEST PISANY O 1 W NOCY I DO ZMIANY JAK WPADNĘ NA LEPSZY POMYSŁ
                if(fields != null) {
                    model.addAttribute("fields", fields);
                    if (fields.contains("chest")) {
                        List<Double> chestMeasurementsList = bodyMeasurementList.stream().mapToDouble(BodyMeasurement::getChest).boxed().toList();
                        model.addAttribute("chestMeasurementList", chestMeasurementsList);
                    }
                    if (fields.contains("shoulders")) {
                        List<Double> shoulderMeasurementsList = bodyMeasurementList.stream().mapToDouble(BodyMeasurement::getShoulders).boxed().toList();
                        model.addAttribute("shouldersMeasurementList", shoulderMeasurementsList);
                    }
                    if (fields.contains("biceps")) {
                        List<Double> bicepsMeasurementsList = bodyMeasurementList.stream().mapToDouble(BodyMeasurement::getBiceps).boxed().toList();
                        model.addAttribute("bicepsMeasurementList", bicepsMeasurementsList);
                    }
                    if (fields.contains("thigh")) {
                        List<Double> thighMeasurementsList = bodyMeasurementList.stream().mapToDouble(BodyMeasurement::getThigh).boxed().toList();
                        model.addAttribute("thighMeasurementList", thighMeasurementsList);
                    }
                    if (fields.contains("waist")) {
                        List<Double> waistMeasurementsList = bodyMeasurementList.stream().mapToDouble(BodyMeasurement::getWaist).boxed().toList();
                        model.addAttribute("waistMeasurementList", waistMeasurementsList);
                    }
                    if (fields.contains("belly")) {
                        List<Double> bellyMeasurementsList = bodyMeasurementList.stream().mapToDouble(BodyMeasurement::getBelly).boxed().toList();
                        model.addAttribute("bellyMeasurementsList", bellyMeasurementsList);
                    }
                    if (fields.contains("hips")) {
                        List<Double> hipsMeasurementsList = bodyMeasurementList.stream().mapToDouble(BodyMeasurement::getHips).boxed().toList();
                        model.addAttribute("hipsMeasurementList", hipsMeasurementsList);
                    }
                }
            }
            case ("exercise") -> {
                WorkoutPlan plan = null;

                model.addAttribute("fields", fields);
                try {
                    plan = planService.getWorkoutPlan(user);
                } catch (Exception e){
                    return "redirect:/plan";
                }
                List<PlanExercise> exercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
                model.addAttribute("exercises", exercises);
               // List<SetLog> setLogList = setLogRepository.find

            }
        }

        return "statistics";
    }
}
