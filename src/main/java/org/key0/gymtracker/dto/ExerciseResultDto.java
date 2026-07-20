package org.key0.gymtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.key0.gymtracker.enums.TrackingParameter;
import org.key0.gymtracker.models.PlanExercise;
import org.key0.gymtracker.models.Training;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ExerciseResultDto {
    Long trainingId;
    List<SetLogDto> setLogs= new ArrayList<>();
    Long exerciseId;

    public ExerciseResultDto(Long trainingId, Integer listSize, Long exerciseId){
        this.trainingId = trainingId;
        this.exerciseId = exerciseId;
        for(int i = 0; i < listSize; i++){
            SetLogDto setLog = new SetLogDto();
            setLog.setSetNumber(i + 1);
            setLogs.add(setLog);
        }
    }
}
