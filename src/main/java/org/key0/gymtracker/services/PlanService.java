package org.key0.gymtracker.services;

import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.dto.WorkoutPlanDto;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PlanService {
    private final WorkoutPlanRepository workoutPlanRepository;
    private final TrainingRepository trainingRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final SetLogRepository setLogRepository;

    public WorkoutPlan getWorkoutPlan(User user){
        WorkoutPlan workoutPlan = workoutPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono planu treningowego dla użytkownika: " + user.getUsername()));
        return workoutPlan;
    }

    public Training getTrainingByWeekAndWorkout(WorkoutPlan plan, Integer week, Integer day){
        List<Training> trainingList = trainingRepository.findByPlanOrderByTrainingDateDesc(plan);
        Optional<Training> training = trainingList.stream()
                .filter(t -> t.getTrainingWeek().equals(week) && t.getDayNumber().equals(day))
                .findFirst();
        return training.orElseThrow(() -> new RuntimeException("Nie znaleziono treningu dla planu: " + plan.getId() + " w tygodniu: " + week));
    }

    public List<SetLog> getSetLogsByWorkoutAndWeek(WorkoutPlan plan, Integer week, Integer day, PlanExercise currentPlanExercise) {
        try {
            Training lastWeekTraining = getTrainingByWeekAndWorkout(plan, week, day);
            if (lastWeekTraining == null) {
                return List.of();
            }

            List<ExerciseResult> lastWeekResults = exerciseResultRepository.findByTraining(lastWeekTraining);

            Optional<ExerciseResult> lastWeekResultOpt = lastWeekResults.stream()
                    .filter(er -> er.getExercise() != null && currentPlanExercise != null
                            && er.getExercise().getId().equals(currentPlanExercise.getId()))
                    .findFirst();

            if (lastWeekResultOpt.isEmpty()) {
                return List.of();
            }

            return setLogRepository.findByExerciseResultOrderBySetNumberAsc(lastWeekResultOpt.get());

        } catch (Exception e) {
            return List.of();
        }
    }

    public void shrinkPlanIfNeeded(WorkoutPlanDto planDto){
        ensureExerciseList(planDto);

        int totalDays = planDto.getDaysPerWeek();
        for (int currentDayCheck = 1; currentDayCheck <= totalDays; currentDayCheck++) {

            final int emptyDayCandidate = currentDayCheck;

            boolean hasExercises = planDto.getPlanExerciseList().stream()
                    .anyMatch(pe -> pe.getDayNumber() == emptyDayCandidate);

            if (!hasExercises) {

                planDto.getPlanExerciseList().stream()
                        .filter(pe -> pe.getDayNumber() > emptyDayCandidate)
                        .forEach(pe -> pe.setDayNumber(pe.getDayNumber() - 1));

                totalDays--;
                planDto.setDaysPerWeek(totalDays);

                currentDayCheck--;
            }
        }
    }

    public void ensureExerciseList(WorkoutPlanDto workoutPlanDto) {
        if (workoutPlanDto.getPlanExerciseList() == null) {
            workoutPlanDto.setPlanExerciseList(new ArrayList<>());
        }
    }

    public WorkoutPlan getOrCreateDefaultPlan(User user) {
        WorkoutPlan plan = workoutPlanRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    WorkoutPlan newPlan = new WorkoutPlan();
                    newPlan.setUser(user);
                    newPlan.setDaysPerWeek(3);
                    WorkoutPlan savedPlan = workoutPlanRepository.save(newPlan);

                    TrackingParameter defaultTrackingParameter = TrackingParameter.REPETITIONS;

                    PlanExercise defaultExercise = new PlanExercise();
                    defaultExercise.setPlan(savedPlan);
                    defaultExercise.setExerciseName("Przykładowe ćwiczenie");
                    defaultExercise.setTargetSets(3);
                    defaultExercise.setTrackingParameter(defaultTrackingParameter);
                    defaultExercise.setNotes("Zmień nazwę i serie, notatki są opcjonalne i pomagają zapamiętać ważne informacje dotyczące ćwiczenia");
                    defaultExercise.setDayNumber(1);
                    defaultExercise.setExerciseNumber(1);
                    planExerciseRepository.save(defaultExercise);
                    return savedPlan;
                });
        return plan;
    }

    public void updateExercises(WorkoutPlanDto planDto, User user){
        WorkoutPlan plan = getWorkoutPlan(user);
        List<PlanExerciseDto> planExerciseDtos = planDto.getPlanExerciseList();
        List<PlanExerciseDto> sortedExercises = planExerciseDtos.stream()
                .sorted(Comparator.comparing(PlanExerciseDto::getDayNumber)
                        .thenComparing(PlanExerciseDto::getExerciseNumber))
                .toList();

        List<PlanExercise> currentExercises = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
        if(currentExercises.size() == planExerciseDtos.size()){
            currentExercises.forEach(pe -> {
                //updatePlanExerciseFromDto(pe, )
            });
        } else if (currentExercises.size() > planExerciseDtos.size()){

        } else {

        }
        // TODO dodać logike aktualizacji ćwiczeń -> jeżeli planExerciseDtos.size() == currentExercises().size() -> nadpisz,
        //  a w innych wypadkach trzeba dodatkowej logiki postępowania z ćwiczeniami nadprogramowymi.
    }

    private void updatePlanExerciseFromDto(PlanExercise pe, PlanExerciseDto peDto){
        pe.setTargetSets(peDto.getTargetSets());
        pe.setExerciseName(peDto.getExerciseName());
        pe.setExerciseNumber(peDto.getExerciseNumber());
        pe.setNotes(peDto.getNotes());
        pe.setDayNumber(peDto.getDayNumber());
        pe.setTrackingParameter(TrackingParameter.fromString(peDto.getTrackingParameter()));
    }
}
