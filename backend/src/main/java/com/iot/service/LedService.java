package com.iot.service;

import com.iot.dto.Dtos.ActionDto;
import com.iot.dto.Dtos.PageDto;
import com.iot.entity.ActionHistory;
import com.iot.entity.Device;
import com.iot.repository.ActionHistoryRepository;
import com.iot.repository.DeviceRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Điều khiển LED: ghi lịch sử (PENDING) -> publish lệnh -> ESP32 gửi snapshot trạng thái
 * -> đánh dấu SUCCESS; quá thời gian chờ thì ERROR.
 */
@Service
public class LedService {
    public static final int LED_COUNT = 3;
    private static final Logger log = LoggerFactory.getLogger(LedService.class);
    private static final Pattern SNAPSHOT = Pattern.compile("led([1-3])\\s*[:=]\\s*\"?(on|off)", Pattern.CASE_INSENSITIVE);

    private final ActionHistoryRepository repo;
    private final DeviceRepository deviceRepo;
    private final MqttGateway mqtt;
    private final String controlTopic, statusTopic;
    private final long timeoutSeconds;

    public LedService(ActionHistoryRepository repo, DeviceRepository deviceRepo, MqttGateway mqtt,
                      @Value("${mqtt.topic.control}") String controlTopic,
                      @Value("${mqtt.topic.action-history}") String statusTopic,
                      @Value("${led.confirm-timeout-seconds}") long timeoutSeconds) {
        this.repo = repo;
        this.deviceRepo = deviceRepo;
        this.mqtt = mqtt;
        this.controlTopic = controlTopic;
        this.statusTopic = statusTopic;
        this.timeoutSeconds = timeoutSeconds;
    }

    @PostConstruct
    void listen() {
        mqtt.subscribe(statusTopic, this::onSnapshot);
    }

    /** Trạng thái hiện tại của LED1..LED3 (lưu trong bảng devices). */
    public boolean[] states() {
        boolean[] states = new boolean[LED_COUNT];
        for (Device d : deviceRepo.findAll()) {
            if (d.getId() >= 1 && d.getId() <= LED_COUNT) states[(int) (d.getId() - 1)] = d.getStatus().equals("ON");
        }
        return states;
    }

    /** @param led led1..led3 hoặc all, @param state on/off */
    @Transactional
    public void control(String led, String state) throws MqttException {
        led = led.toLowerCase();
        boolean on = state.equalsIgnoreCase("on");
        List<Integer> targets = new ArrayList<>();
        if (led.equals("all")) for (int i = 1; i <= LED_COUNT; i++) targets.add(i);
        else targets.add(Integer.parseInt(led.substring(3)));

        LocalDateTime now = LocalDateTime.now();
        List<ActionHistory> rows = targets.stream()
                .map(i -> new ActionHistory(deviceRepo.getReferenceById((long) i), on ? "ON" : "OFF", ActionHistory.PENDING, now)).toList();
        repo.saveAll(rows);
        try {
            mqtt.publish(controlTopic, led + ":" + (on ? "on" : "off"));
        } catch (MqttException e) {
            rows.forEach(r -> r.setStatus(ActionHistory.ERROR));
            repo.saveAll(rows);
            throw e;
        }
    }

    /** Snapshot từ ESP32: led1:on,led2:off,led3:on (chấp nhận cả JSON cũ). */
    void onSnapshot(String payload) {
        Matcher m = SNAPSHOT.matcher(payload);
        boolean[] seen = new boolean[LED_COUNT];
        boolean[] on = new boolean[LED_COUNT];
        while (m.find()) {
            int i = Integer.parseInt(m.group(1)) - 1;
            on[i] = m.group(2).equalsIgnoreCase("on");
            seen[i] = true;
        }
        List<Device> changed = new ArrayList<>();
        for (Device d : deviceRepo.findAll()) {
            int i = (int) (d.getId() - 1);
            if (i >= 0 && i < LED_COUNT && seen[i] && !d.getStatus().equals(on[i] ? "ON" : "OFF")) {
                d.setStatus(on[i] ? "ON" : "OFF");
                changed.add(d);
            }
        }
        deviceRepo.saveAll(changed);

        List<ActionHistory> confirmed = new ArrayList<>();
        for (ActionHistory a : repo.findByStatusOrderByIdAsc(ActionHistory.PENDING)) {
            int i = (int) (a.getDevice().getId() - 1);
            if (seen[i] && on[i] == a.getAction().equals("ON")) {
                a.setStatus(ActionHistory.SUCCESS);
                confirmed.add(a);
            }
        }
        repo.saveAll(confirmed);
    }

    @Scheduled(fixedDelay = 1000)
    void expirePending() {
        List<ActionHistory> late = repo.findByStatusAndCreatedAtBefore(ActionHistory.PENDING,
                LocalDateTime.now().minusSeconds(timeoutSeconds));
        if (late.isEmpty()) return;
        log.warn("{} lệnh LED không được ESP32 xác nhận", late.size());
        late.forEach(a -> a.setStatus(ActionHistory.ERROR));
        repo.saveAll(late);
    }

    public PageDto<ActionDto> history(int page, int size, String q, Long led, String action, String status) {
        Specification<ActionHistory> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                Expression<String> time = cb.function("DATE_FORMAT", String.class, root.get("createdAt"),
                        cb.literal("%d/%m/%Y %H:%i:%s"));
                ps.add(cb.like(time, "%" + q.trim() + "%"));
            }
            if (led != null) ps.add(cb.equal(root.get("device").get("id"), led));
            if (action != null && !action.isBlank()) ps.add(cb.equal(root.get("action"), action));
            if (status != null && !status.isBlank()) ps.add(cb.equal(root.get("status"), status));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return PageDto.of(repo.findAll(spec, PageRequest.of(Math.max(page, 1) - 1, size, Sort.by("id").descending())),
                ActionDto::of);
    }
}
