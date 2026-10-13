package com.iot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "devices")
public class Device {
    @Id
    private Long id;
    private String name;
    private String status; // ON | OFF

    protected Device() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
