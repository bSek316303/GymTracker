package org.key0.gymtracker.services;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.key0.gymtracker.dto.ExerciseResultDto;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.dto.WorkoutPlanDto;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.*;
import org.key0.gymtracker.repositories.*;
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
        return planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId())
                .stream()
                .collect(Collectors.toMap(
                        pe -> String.valueOf(pe.getDayNumber()) + "-" + String.valueOf(pe.getExerciseNumber()),
                        pe -> pe
                ));
    }

    private void updatePlanExerciseFromDto(PlanExercise pe, PlanExerciseDto peDto){
        pe.setTargetSets(peDto.getTargetSets());
        pe.setExerciseName(peDto.getExerciseName());
        pe.setExerciseNumber(peDto.getExerciseNumber());
        pe.setGuidelines(peDto.getGuidelines());
        pe.setDayNumber(peDto.getDayNumber());
        pe.setTrackingParameter(TrackingParameter.fromString(peDto.getTrackingParameter()));
    }

    // Metoda nadpisuje plan treningowy użytkownika w taki sposób, żeby wyniki dotychczasowych ćwiczeń zostały.
    // Czyli np. jeżeli użytkownik robił ławkę płaską i zmienił plan to zostaną mu informacje o jego wynikach w tym ćwiczeniu
    // choćby je przeniósł na inny dzień.

    // Metoda zakłada, że użytkownik nie będzie miał 2 tych samych ćwiczeń jako 2 różne ćwiczenia tego samego dnia co nie ma sensu z perspektywy użytkownika siłowni
    @Transactional
    public void filterExercises(WorkoutPlanDto workoutPlanDto, WorkoutPlan plan){
        List<PlanExercise> exercisesToFilter = planExerciseRepository.findByPlanIdOrderByExerciseNumberAsc(plan.getId());
        List<PlanExercise> exercisesToSave = new ArrayList<>();

        for(PlanExerciseDto exDto : workoutPlanDto.getPlanExerciseList()){
            Optional<PlanExercise> exerciseMatch = exercisesToFilter.stream()
                    .filter(ex -> exDto.getExerciseName().equalsIgnoreCase(ex.getExerciseName()) && ex.getDayNumber() == exDto.getDayNumber()).findAny();
            if(exerciseMatch.isPresent()){
                // W tym przypadku istnieje ćwiczenie z tą nazwą tego samego dnia więc użytkownik go nie ruszał bądź zamienił ćwiczenia miejscami
                // Sprawdźmy najpierw, czy ćwiczenia zostały zamienione miejscami.
                if(exDto.getExerciseNumber() == exerciseMatch.get().getExerciseNumber()){
                    // Tutaj nie zostały zamienione miejscami więc to ćwiczenie jest sprawdzone do nadpisania
                    exerciseMatch.get().updateFromPlanExerciseDto(exDto);
                    exercisesToSave.add(exerciseMatch.get());
                    continue;
                } else {
                    // Tutaj musimy zamienić numerami ćwiczenia.
                    Optional<PlanExercise> exerciseToChangeExerciseNumber = exercisesToFilter.stream()
                            .filter(ex-> ex.getDayNumber() == exDto.getDayNumber() && exDto.getExerciseNumber() == ex.getExerciseNumber()).findFirst();
                    exerciseToChangeExerciseNumber.ifPresent(ex -> ex.setExerciseNumber(-1));
                    exerciseMatch.get().updateFromPlanExerciseDto(exDto);
                    exercisesToSave.add(exerciseMatch.get());
                    continue;
                }
                // W tym przypadku sprawdzimy, czy w innym dniu nie ma takiego ćwiczenia.
            } else {
                // Używamy listy, ponieważ użytkownik może mieć to samo ćwiczenie kilka razy w ciągu tygodnia więc znajdziemy je wszystkie i sprawdzimy
                // czy lista dto ćwiczeń nie zawiera takiego ćwiczenia w danym dniu
                // np -> mamy w ex dto ławkę płaską w 1 dzień. W ćwiczeniach dotychczasowych nie ma w 1 dniu takiego ćwiczenia więc szukamy w innych.
                // Znajdujemy w 3 i 4 dniu takie ćwiczenie (stąd lista). Jeżeli dto ćwiczeń ma w 4 dniu też takie ćwiczenie to nie możemy podebrać informacji
                // o wynikach z tego ćwiczenia. Natomiast jeżeli w 3 dniu nie będzie takiego ćwiczenia to znaczy, że użytkownik przeniósł to ćwiczenie na 1 dzień
                // i w takim przypadku pobieramy informacje o wynikach i aktualizujemy.
                List<PlanExercise> exerciseMatchesFromOtherDays = exercisesToFilter.stream()
                        .filter(ex -> exDto.getExerciseName().equalsIgnoreCase(ex.getExerciseName())).toList();
                boolean wasFound = false;
                for(PlanExercise exerciseMatchFromOtherDays : exerciseMatchesFromOtherDays){
                    boolean isDuplicate = workoutPlanDto.getPlanExerciseList().stream()
                            .anyMatch(planExDto -> Objects.equals(planExDto.getExerciseName(), exerciseMatchFromOtherDays.getExerciseName())
                            && exerciseMatchFromOtherDays.getDayNumber() == planExDto.getDayNumber());
                    if(!isDuplicate){
                        // Teraz wiemy, że w tamtym dniu nie ma takiego ćwiczenia więc zostało przeniesione.
                        // Musimy jeszcze sprawdzić, czy nie zostało przeniesione kilka razy. Żeby to sprawdzić, po prostu zobaczymy
                        // czy nie ma takiego ćwiczenia w exercisesToSave;
                        boolean isSaved = exercisesToSave.stream().anyMatch(exercise -> exercise.getId() == exerciseMatchFromOtherDays.getId());
                        if(!isSaved){
                            // Teraz już jesteśmy pewni, że to nie jest ani duplikat, ani wyniki z tego ćwiczenia nie zostały przypisane do innego
                            exerciseMatchFromOtherDays.updateFromPlanExerciseDto(exDto);
                            exercisesToSave.add(exerciseMatchFromOtherDays);
                            wasFound = true;
                            break;
                        }
                    }
                }
                if(wasFound) continue;
            }
            // Teraz już wiemy, że nie ma takiego ćwiczenia w dotychczasowej bazie ćwiczeń więc musimy dodać nowe ćwiczenie.
            PlanExercise newExercise = new PlanExercise();
            newExercise.updateFromPlanExerciseDto(exDto);
            newExercise.setPlan(plan);
            exercisesToSave.add(newExercise);
            exercisesToFilter.add(newExercise);
        }
        // Pętla zapisała wszystkie exDto więc zostaje nam nadpisać bazę.
        exercisesToFilter.removeIf(ex -> exercisesToSave.stream()
                .anyMatch(saved -> saved.getId() != null && saved.getId().equals(ex.getId())));
        planExerciseRepository.deleteAll(exercisesToFilter);
        planExerciseRepository.saveAll(exercisesToSave);

        int days = 0;
        for(PlanExercise ex : exercisesToSave) if(ex.getDayNumber() > days) days = ex.getDayNumber();
        plan.setDaysPerWeek(days);
        workoutPlanRepository.save(plan);
    }
}
