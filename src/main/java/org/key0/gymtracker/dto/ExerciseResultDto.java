package org.key0.gymtracker.dto;

import lombok.Data;
import org.key0.gymtracker.enums.TrackingParameter;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExerciseResultDto {
    Long trainingId;
    List<SetLogDto> setLogs= new ArrayList<>();
    TrackingParameter trackingParameter;

    public ExerciseResultDto(Long trainingId, Integer listSize, TrackingParameter trackingParameter){
        this.trainingId = trainingId;
        this.trackingParameter = trackingParameter;
        for(int i = 0; i < listSize; i++){
            SetLogDto setLog = new SetLogDto();
            setLog.setSetNumber(i + 1);
            setLogs.add(setLog);
        }
    }
}
