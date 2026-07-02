package org.key0.gymtracker.dto;

import lombok.Data;
import org.key0.gymtracker.dto.PlanExerciseDto;
import org.key0.gymtracker.repositories.PlanExerciseRepository;

import java.io.Serializable;
import java.util.List;

@Data
public class WorkoutPlanDto implements Serializable{
    private static final long serialVersionUID = 1L;
    private int daysPerWeek;
    private List<PlanExerciseDto> planExerciseList;
};