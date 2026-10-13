package com.iot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "action_history")
public class ActionHistory {
    public static final String PENDING = "PENDING", SUCCESS = "SUCCESS", ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "device_id")
    private Device device;
    @Column(name = "user_id")
    private Long userId = 1L; // chưa có đăng nhập nên mọi lệnh đều thuộc người dùng 1
    private String action; // ON | OFF
    private String status;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected ActionHistory() {}

    public ActionHistory(Device device, String action, String status, LocalDateTime createdAt) {
        this.device = device;
        this.action = action;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Device getDevice() { return device; }
    public String getAction() { return action; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
