package com.iot.dto;

import com.iot.entity.ActionHistory;
import com.iot.entity.SensorData;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.data.domain.Page;

public final class Dtos {
    private Dtos() {}

    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /** Một lần đo đủ 3 cảm biến (dùng cho thẻ số liệu và biểu đồ). */
    public record SensorDto(Double temperature, Double humidity, Double light, String time) {}

    /** Một dòng của bảng dữ liệu: giá trị của một cảm biến. */
    public record SensorRowDto(Long id, String sensor, String unit, double value, String time) {
        public static SensorRowDto of(SensorData s) {
            return new SensorRowDto(s.getId(), s.getSensor().getName(), s.getSensor().getUnit(),
                    s.getValue().doubleValue(), s.getCreatedAt().format(TIME));
        }
    }

    /** led = id thiết bị (1..3). */
    public record ActionDto(Long id, long led, String device, String action, String status, String time) {
        public static ActionDto of(ActionHistory a) {
            return new ActionDto(a.getId(), a.getDevice().getId(), a.getDevice().getName(), a.getAction(),
                    a.getStatus(), a.getCreatedAt().format(TIME));
        }
    }

    public record ProfileDto(String name, String description) {}

    public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <E, T> PageDto<T> of(Page<E> p, java.util.function.Function<E, T> map) {
            return new PageDto<>(p.getContent().stream().map(map).toList(), p.getNumber() + 1, p.getSize(),
                    p.getTotalElements(), p.getTotalPages());
        }
    }

    /** led: led1 | led2 | led3 | all; state: on | off */
    public record LedControlRequest(
            @NotNull @Pattern(regexp = "(?i)led[1-3]|all", message = "led phải là led1, led2, led3 hoặc all") String led,
            @NotNull @Pattern(regexp = "(?i)on|off", message = "state phải là on hoặc off") String state) {}
}
