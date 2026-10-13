package com.iot.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_data")
public class SensorData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;
    private BigDecimal value;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected SensorData() {}

    public SensorData(Sensor sensor, BigDecimal value, LocalDateTime createdAt) {
        this.sensor = sensor;
        this.value = value;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Sensor getSensor() { return sensor; }
    public BigDecimal getValue() { return value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
