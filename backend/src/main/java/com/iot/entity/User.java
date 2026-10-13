package com.iot.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;
    private String name;
    private String description;

    protected User() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
}
