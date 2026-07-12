package org.key0.gymtracker.services;

import lombok.AllArgsConstructor;
import org.hibernate.jdbc.Work;
import org.key0.gymtracker.dto.ExerciseResultDto;
import org.key0.gymtracker.dto.SetLogDto;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.ExerciseResultRepository;
import org.key0.gymtracker.repositories.PlanExerciseRepository;
import org.key0.gymtracker.repositories.SetLogRepository;
import org.key0.gymtracker.repositories.TrainingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TrainingService {
    private final PlanService planService;
    private final TrainingRepository trainingRepository;
    private final PlanExerciseRepository planExerciseRepository;
    private final ExerciseResultRepository exerciseResultRepository;
    private final SetLogRepository setLogRepository;

    public boolean isUsersTrainingWeekFinished(User user, Integer trainingWeek){
        WorkoutPlan plan = planService.getWorkoutPlan(user);
        List<Training> userSessions = trainingRepository.findByPlanOrderByTrainingDateDesc(plan);

        if (userSessions == null || userSessions.isEmpty()) return false;
        if(userSessions.getFirst().getTrainingWeek() > trainingWeek) return true;

        return userSessions.stream().filter(training -> trainingWeek.equals(training.getTrainingWeek())).count() == plan.getDaysPerWeek();
    }

    public Training getTraining(int weekNumber, int dayNumber, WorkoutPlan workoutPlan){
        return trainingRepository.findByTrainingWeekAndDayNumberAndPlan(weekNumber, dayNumber, workoutPlan)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono aktywnego treningu."));
    }

    public Training getOrCreateTraining(int weekNumber, int dayNumber, WorkoutPlan workoutPlan){
        Optional<Training> oTraining = trainingRepository.findByTrainingWeekAndDayNumberAndPlan(weekNumber, dayNumber, workoutPlan);

        // W przypadku rozpoczęcia nowego treningu przygotowujemy entity Training.
        if(oTraining.isEmpty()){
            Training newTraining = new Training();
            newTraining.setTrainingDate(java.time.LocalDate.now());
            newTraining.setTrainingWeek(weekNumber);
            newTraining.setDayNumber(dayNumber);
            newTraining.setPlan(workoutPlan);
            trainingRepository.save(newTraining);
            oTraining = Optional.of(newTraining);
        }

        return oTraining.get();
    }

    @Transactional
    public void prepareTraining(Training training){
        // Metoda ma przygotować wszystkie ExerciseResult oraz SetLogi do nowego lub skończonego treningu do którego użytkownik wrócił
        // Edge case -> użytkownik skończył trening zwiekszył ilość serii, a następnie wrocił do starego treningu.
        // Przy zmniejszeniu ilości serii program automatycznie usuwa nadmierne set logi więc to nie edge case.
        WorkoutPlan workoutPlan = training.getPlan();

        List<PlanExercise> planExerciseList = planExerciseRepository.findByPlanIdAndDayNumberOrderByExerciseNumberAsc(workoutPlan.getId(), training.getDayNumber());
        List<ExerciseResult> exerciseResultList = exerciseResultRepository.findByTraining(training);
        List<SetLog> setLogList = setLogRepository.findByExerciseResultIn(exerciseResultList);

        for (PlanExercise planEx : planExerciseList) {
            ExerciseResult result = exerciseResultList.stream()
                    .filter(er -> er.getExercise().equals(planEx))
                    .findFirst()
                    .orElse(null);

            if (result == null) {
                result = new ExerciseResult(training, planEx);
                exerciseResultRepository.save(result);
                exerciseResultList.add(result);
            }

            final ExerciseResult finalResult = result;

            List<SetLog> logsForExercise = setLogList.stream()
                    .filter(sl -> sl.getExerciseResult().equals(finalResult))
                    .toList();

            int currentSetCount = logsForExercise.size();
            int targetSets = planEx.getTargetSets();

            if (currentSetCount < targetSets) {
                List<Integer> existingSetNumbers = logsForExercise.stream()
                        .map(SetLog::getSetNumber)
                        .toList();

                for (int i = 1; i <= targetSets; i++) {
                    if (!existingSetNumbers.contains(i)) {
                        SetLog newLog = new SetLog();
                        newLog.setExerciseResult(result);
                        newLog.setSetNumber(i);
                        setLogRepository.save(newLog);
                    }
                }
            }
        }
    }

    public Map<Integer, ExerciseResultDto> getExerciseResultDtoMap(Training training){
        WorkoutPlan workoutPlan = training.getPlan();
        List<PlanExercise> planExerciseList = planExerciseRepository.findByPlanIdAndDayNumberOrderByExerciseNumberAsc(workoutPlan.getId(), training.getDayNumber());

        // 1. Pobieramy wszystkie rezultaty i wszystkie logi jednym zapytaniem
        List<ExerciseResult> exerciseResultList = exerciseResultRepository.findByTraining(training);
        List<SetLog> allLogs = setLogRepository.findByExerciseResultIn(exerciseResultList);

        return planExerciseList.stream().map(ex -> {
            // 2. Tworzymy puste DTO
            ExerciseResultDto dto = new ExerciseResultDto(training, planExerciseList.size(), ex);

            // 3. Znajdujemy encję ExerciseResult dla tego ćwiczenia
            ExerciseResult er = exerciseResultList.stream()
                    .filter(result -> result.getExercise().equals(ex))
                    .findFirst()
                    .orElse(null);

            if (er != null) {
                // 4. Wyciągamy logi dla tego rezultatu, mapujemy na DTO i sortujemy
                List<SetLogDto> logDtos = allLogs.stream()
                        .filter(log -> log.getExerciseResult().equals(er))
                        .map(log -> new SetLogDto(log)) // Zakładam, że masz konstruktor SetLogDto(SetLog log)
                        .sorted(Comparator.comparingInt(SetLogDto::getSetNumber))
                        .collect(Collectors.toList());

                // 5. Wkładamy gotowe serie do DTO
                dto.setSetLogs(logDtos);
            }

            return dto;
        }).collect(Collectors.toMap(exDto -> exDto.getExercise().getExerciseNumber(), exDto -> exDto));
    }

    public Map<Integer, ExerciseResult> getExerciseResultMap(Training training){
        WorkoutPlan workoutPlan = training.getPlan();
        List<ExerciseResult> exerciseResultList = exerciseResultRepository.findByTraining(training);

        return exerciseResultList.stream()
                .collect(Collectors.toMap(exDto -> exDto.getExercise().getExerciseNumber(), exDto -> exDto));
    }

    @Transactional
    public void saveExerciseResultDto(ExerciseResultDto exerciseResultDto) {
        ExerciseResult exerciseResult = exerciseResultRepository.findByTrainingAndExercise(exerciseResultDto.getTraining(), exerciseResultDto.getExercise())
                .orElseThrow(() -> new IllegalArgumentException("Wynik ćwiczenia nie istnieje"));

        List<SetLog> setLogList = setLogRepository.findByExerciseResultOrderBySetNumberAsc(exerciseResult);

        List<SetLogDto> setLogDtoList = exerciseResultDto.getSetLogs().stream()
                .sorted(Comparator.comparingInt(SetLogDto::getSetNumber))
                .toList();
        for (int i = 0; i < setLogList.size(); i++) {
            SetLog entity = setLogList.get(i);
            SetLogDto dto = setLogDtoList.get(i);

            entity.setValueByParameter(dto.getParameter(), exerciseResultDto.getExercise().getTrackingParameter());
            entity.setWeight(dto.getWeight());
            entity.setRir(dto.getRir());
            entity.setRestTime(dto.getRestTime());
        }
    }
}
