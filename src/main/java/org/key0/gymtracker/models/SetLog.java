package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Data;
import org.key0.gymtracker.dto.SetLogDto;
import org.key0.gymtracker.enums.TrackingParameter;

@Entity
@Table(name="set_logs")
@Data
public class SetLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="set_number", nullable = false)
    private Integer setNumber;

    @Column(name="weight", nullable = true)
    private Double weight;

    @Column(name="reps", nullable = true)
    private Integer reps;

    @Column(name="duration_seconds", nullable = true)
    private Integer durationSeconds;

    @Column(name="distance_meters", nullable = true)
    private Integer distanceMeters;

    @Column(name="calories", nullable = true)
    private Integer calories;

    @Column(name="rir", nullable = true)
    private Integer rir;

    @Column(name="rest_time", nullable = true)
    private Integer restTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_result_id", nullable = false)
    private ExerciseResult exerciseResult;

    public void setParameter(Integer value){
        switch (this.exerciseResult.getExercise().getTrackingParameter()) {
            case TrackingParameter.REPETITIONS -> {
                this.reps = value;
                this.durationSeconds = null;
                this.distanceMeters = null;
                this.calories = null;
            }
            case TrackingParameter.DISTANCE -> {
                this.reps = null;
                this.durationSeconds = null;
                this.distanceMeters = value;
                this.calories = null;
            }
            case TrackingParameter.TIME -> {
                this.reps = null;
                this.durationSeconds = value;
                this.distanceMeters = null;
                this.calories = null;
            }
            case TrackingParameter.CALORIES -> {
                this.reps = null;
                this.durationSeconds = null;
                this.distanceMeters = null;
                this.calories = value;
            }
        }
    }

    public Integer getParameter(){
        switch (this.exerciseResult.getExercise().getTrackingParameter()) {
            case TrackingParameter.REPETITIONS -> { return this.reps; }
            case TrackingParameter.DISTANCE -> { return this.distanceMeters; }
            case TrackingParameter.TIME -> { return this.durationSeconds; }
            case TrackingParameter.CALORIES -> { return this.calories; }
            default -> { return null; }
        }
    }

    public void updateFromSetLogDto(SetLogDto setLogDto){
        this.setNumber = setLogDto.getSetNumber();
        this.setParameter(setLogDto.getParameter());
        this.weight = setLogDto.getWeight();
        this.restTime = setLogDto.getRestTime();
        this.rir = setLogDto.getRir();
    }
}
