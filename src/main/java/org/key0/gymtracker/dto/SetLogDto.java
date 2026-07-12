package org.key0.gymtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.key0.gymtracker.models.SetLog;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetLogDto {
    Integer setNumber;
    Integer parameter;
    Double weight;
    Integer rir;
    Integer restTime;

    public SetLog toSetLogWithoutParameter(){
        SetLog setLog = new SetLog();
        setLog.setSetNumber(this.setNumber);
        setLog.setWeight(this.weight);
        setLog.setRir(this.rir);
        setLog.setRestTime(this.restTime);
        return setLog;
    }

    public boolean isEmpty(){
        return parameter == null || parameter.equals(0);
    }

    public SetLogDto(SetLog setLog){
        this.setNumber = setLog.getSetNumber();
        this.weight = setLog.getWeight();
        this.parameter = setLog.getParameter();
        this.rir = setLog.getRir();
        this.restTime = setLog.getRestTime();
    }
}
