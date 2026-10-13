package com.iot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sensors")
public class Sensor {
    @Id
    private Long id;
    private String name;
    private String unit;

    protected Sensor() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
}
