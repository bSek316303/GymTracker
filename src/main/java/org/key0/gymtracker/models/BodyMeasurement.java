package org.key0.gymtracker.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "body_measurements")
@Data
public class BodyMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "measurement_date", nullable = false)
    private LocalDate measurementDate;

    @Column(name = "chest")
    private Double chest;

    @Column(name = "shoulders")
    private Double shoulders;

    @Column(name = "biceps")
    private Double biceps;

    @Column(name = "thigh")
    private Double thigh;

    @Column(name = "waist")
    private Double waist;

    @Column(name = "belly")
    private Double belly;

    @Column(name = "hips")
    private Double hips;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public boolean validValues(){
        return (chest >= 0) &&
               (shoulders >= 0) &&
               (biceps >= 0) &&
               (thigh >= 0) &&
               (waist >= 0) &&
               (belly >= 0) &&
               (hips >= 0);
    }
}
