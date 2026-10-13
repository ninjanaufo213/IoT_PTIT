package com.iot.controller;

import com.iot.dto.Dtos.*;
import com.iot.repository.UserRepository;
import com.iot.service.LedService;
import com.iot.service.SensorDataService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ApiController {
    private final SensorDataService sensors;
    private final LedService leds;
    private final UserRepository users;

    public ApiController(SensorDataService sensors, LedService leds, UserRepository users) {
        this.users = users;
        this.sensors = sensors;
        this.leds = leds;
    }

    /** Mẫu cảm biến mới nhất (204 nếu chưa có dữ liệu). */
    @GetMapping("/sensor-data")
    public ResponseEntity<SensorDto> latest() {
        return sensors.latest().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/sensor-data/chart")
    public java.util.List<SensorDto> chart(@RequestParam(defaultValue = "20") int limit) {
        return sensors.chart(Math.min(Math.max(limit, 1), 500));
    }

    @GetMapping("/sensor-data-list")
    public PageDto<SensorRowDto> sensorList(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(defaultValue = "all") String field,
                                         @RequestParam(required = false) String q) {
        return sensors.search(page, Math.min(Math.max(size, 1), 100), field, q);
    }

    /** {"led1":true,"led2":false,"led3":false} */
    @GetMapping("/led-status")
    public Map<String, Boolean> ledStatus() {
        boolean[] s = leds.states();
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (int i = 0; i < s.length; i++) out.put("led" + (i + 1), s[i]);
        return out;
    }

    @PostMapping("/led-control")
    public Map<String, String> ledControl(@Valid @RequestBody LedControlRequest req) throws MqttException {
        leds.control(req.led(), req.state());
        return Map.of("message", "Đã gửi lệnh " + req.led().toLowerCase() + ":" + req.state().toLowerCase());
    }

    @GetMapping("/action-history")
    public PageDto<ActionDto> history(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Long led,
                                      @RequestParam(required = false) String action,
                                      @RequestParam(required = false) String status) {
        return leds.history(page, Math.min(Math.max(size, 1), 100), q, led, action, status);
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileDto> profile() {
        return users.findFirstByOrderByIdAsc().map(u -> ResponseEntity.ok(new ProfileDto(u.getName(), u.getDescription())))
                .orElse(ResponseEntity.noContent().build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> invalid(MethodArgumentNotValidException e) {
        return Map.of("message", e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }

    @ExceptionHandler(MqttException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    Map<String, String> mqttDown(MqttException e) {
        return Map.of("message", "Không gửi được lệnh: MQTT broker chưa kết nối");
    }
}
