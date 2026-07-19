package org.key0.gymtracker.services;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.dto.WorkoutPlanDto;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.*;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PlanService {
    private final WorkoutPlanRepository workoutPlanRepository;
    private final TrainingRepository trainingRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final SetLogRepository setLogRepository;

    public WorkoutPlan getWorkoutPlan(User user){
        return workoutPlanRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Nie znaleziono planu treningowego dla użytkownika: " + user.getUsername()));
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

    @Transactional
    public WorkoutPlan getOrCreateDefaultPlan(User user) {
        WorkoutPlan plan = workoutPlanRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    WorkoutPlan newPlan = new WorkoutPlan();
                    newPlan.setUser(user);
                    newPlan.setDaysPerWeek(3);
                    newPlan.setLastEditDate(LocalDate.now());
                    WorkoutPlan savedPlan = workoutPlanRepository.save(newPlan);

                    TrackingParameter defaultTrackingParameter = TrackingParameter.REPETITIONS;

                    for(int i = 1; i <= newPlan.getDaysPerWeek(); i++) {
                        PlanExercise defaultExercise = new PlanExercise();
                        defaultExercise.setPlan(savedPlan);
                        defaultExercise.setExerciseName("Przykładowe ćwiczenie");
                        defaultExercise.setTargetSets(3);
                        defaultExercise.setTrackingParameter(defaultTrackingParameter);
                        defaultExercise.setGuidelines("Zmień nazwę i serie, notatki są opcjonalne i pomagają zapamiętać ważne informacje dotyczące ćwiczenia");
                        defaultExercise.setDayNumber(i);
                        defaultExercise.setExerciseNumber(1);
                        planExerciseRepository.save(defaultExercise);
                    }
                    return savedPlan;
                });
        return plan;
    }

    public void updateExercises(WorkoutPlanDto planDto, User user) {
        WorkoutPlan plan = getWorkoutPlan(user);
        Map<String, PlanExercise> oldExercisesMap = getPlanExercisesMap(user);

        List<PlanExercise> exercisesToSave = planDto.getPlanExerciseList().stream()
                .map(dto -> {
                    String key = String.valueOf(dto.getDayNumber()) + "-" + String.valueOf(dto.getExerciseNumber());

                    PlanExercise existing = oldExercisesMap.remove(key);

                    if (existing != null) {
                        updatePlanExerciseFromDto(existing, dto);
                        return existing;
                    } else {
                        PlanExercise newPe = dto.toPlanExerciseWithoutPlan();
                        newPe.setPlan(plan);
                        return newPe;
                    }
                })
                .toList();

        Collection<PlanExercise> exercisesToDelete = oldExercisesMap.values();

        if (!exercisesToSave.isEmpty()) {
            planExerciseRepository.saveAll(exercisesToSave);
        }
        if (!exercisesToDelete.isEmpty()) {
            planExerciseRepository.deleteAll(exercisesToDelete);
        }
    }

    public void updateSessionFromForm(Integer day, WorkoutPlanDto updatedPlanDto, HttpSession httpSession) {
        WorkoutPlanDto currentSessionDto = (WorkoutPlanDto) httpSession.getAttribute("workoutPlanDto");

        if (currentSessionDto == null) return;

        ensureExerciseList(currentSessionDto);
        currentSessionDto.getPlanExerciseList().removeIf(Objects::isNull);

        if (day != null) {
            if (updatedPlanDto != null && updatedPlanDto.getPlanExerciseList() != null) {
                currentSessionDto.getPlanExerciseList().removeIf(ex -> day.equals(ex.getDayNumber()));

                List<PlanExerciseDto> validExercisesFromForm = updatedPlanDto.getPlanExerciseList().stream()
                        .filter(Objects::nonNull)
                        .filter(ex -> ex.getExerciseName() != null && !ex.getExerciseName().trim().isEmpty())
                        .filter(ex -> day.equals(ex.getDayNumber()))
                        .toList();

                currentSessionDto.getPlanExerciseList().addAll(validExercisesFromForm);
            }

            boolean isDayEmpty = currentSessionDto.getPlanExerciseList().stream()
                    .noneMatch(ex -> day.equals(ex.getDayNumber()));

            if (isDayEmpty && currentSessionDto.getDaysPerWeek() > 1) {

                currentSessionDto.setDaysPerWeek(currentSessionDto.getDaysPerWeek() - 1);

                currentSessionDto.getPlanExerciseList().stream()
                        .filter(ex -> ex.getDayNumber() > day)
                        .forEach(ex -> ex.setDayNumber(ex.getDayNumber() - 1));
            }
        }

        currentSessionDto.getPlanExerciseList().removeIf(ex -> ex.getDayNumber() > currentSessionDto.getDaysPerWeek());
        httpSession.setAttribute("workoutPlanDto", currentSessionDto);
    }

    public Map<String, PlanExercise> getPlanExercisesMap(User user){
        WorkoutPlan plan = getWorkoutPlan(user);
        Map<String, PlanExercise> mapToReturn = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId())
                .stream()
                .collect(Collectors.toMap(
                        pe -> String.valueOf(pe.getDayNumber()) + "-" + String.valueOf(pe.getExerciseNumber()),
                        pe -> pe
                ));
        return mapToReturn;
    }

    private void updatePlanExerciseFromDto(PlanExercise pe, PlanExerciseDto peDto){
        pe.setTargetSets(peDto.getTargetSets());
        pe.setExerciseName(peDto.getExerciseName());
        pe.setExerciseNumber(peDto.getExerciseNumber());
        pe.setGuidelines(peDto.getGuidelines());
        pe.setDayNumber(peDto.getDayNumber());
        pe.setTrackingParameter(TrackingParameter.fromString(peDto.getTrackingParameter()));
    }
}
