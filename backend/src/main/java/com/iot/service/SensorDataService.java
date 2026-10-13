package com.iot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.dto.Dtos.PageDto;
import com.iot.dto.Dtos.SensorDto;
import com.iot.dto.Dtos.SensorRowDto;
import com.iot.entity.SensorData;
import com.iot.repository.SensorDataRepository;
import com.iot.repository.SensorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Expression;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class SensorDataService {
    private static final Logger log = LoggerFactory.getLogger(SensorDataService.class);

    // Khóa JSON của payload -> id cảm biến trong bảng sensors (xem database/init.sql)
    private static final Map<String, Long> SENSOR_IDS = Map.of("temperature", 1L, "humidity", 2L, "light", 3L);

    private final SensorDataRepository repo;
    private final SensorRepository sensorRepo;
    private final MqttGateway mqtt;
    private final ObjectMapper mapper;
    private final String dataTopic;

    public SensorDataService(SensorDataRepository repo, SensorRepository sensorRepo, MqttGateway mqtt, ObjectMapper mapper,
                             @Value("${mqtt.topic.data}") String dataTopic) {
        this.repo = repo;
        this.sensorRepo = sensorRepo;
        this.mqtt = mqtt;
        this.mapper = mapper;
        this.dataTopic = dataTopic;
    }

    @PostConstruct
    void listen() {
        mqtt.subscribe(dataTopic, this::onMessage);
    }

    /** Payload: {"temperature":28.5,"humidity":75.0,"light":100} - timestamp do backend gán. */
    void onMessage(String payload) {
        try {
            JsonNode n = mapper.readTree(payload);
            for (String key : SENSOR_IDS.keySet()) {
                if (!n.path(key).isNumber()) {
                    log.warn("Bỏ qua dữ liệu cảm biến không hợp lệ: {}", payload);
                    return;
                }
            }
            // 3 dòng cùng một mốc thời gian để ghép lại thành một lần đo
            LocalDateTime now = LocalDateTime.now();
            SENSOR_IDS.forEach((key, id) -> repo.save(
                    new SensorData(sensorRepo.getReferenceById(id), BigDecimal.valueOf(n.get(key).asDouble()), now)));
        } catch (Exception e) {
            log.warn("Bỏ qua bản tin cảm biến lỗi định dạng: {}", payload);
        }
    }

    /** Giá trị mới nhất của từng cảm biến (rỗng nếu chưa có dữ liệu). */
    public Optional<SensorDto> latest() {
        Optional<SensorData> t = repo.findFirstBySensorIdOrderByIdDesc(1L);
        Optional<SensorData> h = repo.findFirstBySensorIdOrderByIdDesc(2L);
        Optional<SensorData> l = repo.findFirstBySensorIdOrderByIdDesc(3L);
        if (t.isEmpty() && h.isEmpty() && l.isEmpty()) return Optional.empty();
        return Optional.of(new SensorDto(t.map(d -> d.getValue().doubleValue()).orElse(null),
                h.map(d -> d.getValue().doubleValue()).orElse(null),
                l.map(d -> d.getValue().doubleValue()).orElse(null),
                t.map(d -> d.getCreatedAt().format(com.iot.dto.Dtos.TIME)).orElse(null)));
    }

    /** N lần đo mới nhất, cũ -> mới, ghép các dòng cùng created_at. */
    public List<SensorDto> chart(int limit) {
        Map<LocalDateTime, Double[]> groups = new LinkedHashMap<>();
        for (SensorData d : repo.findAll(PageRequest.of(0, limit * SENSOR_IDS.size(), Sort.by("id").descending()))) {
            groups.computeIfAbsent(d.getCreatedAt(), k -> new Double[3])[(int) (d.getSensor().getId() - 1)] =
                    d.getValue().doubleValue();
        }
        List<SensorDto> list = new java.util.ArrayList<>();
        groups.forEach((t, v) -> list.add(new SensorDto(v[0], v[1], v[2], t.format(com.iot.dto.Dtos.TIME))));
        Collections.reverse(list);
        return list.size() > limit ? list.subList(list.size() - limit, list.size()) : list;
    }

    /** field: all (tìm theo giá trị) | time | tên cảm biến (tìm theo giá trị của riêng cảm biến đó). */
    public PageDto<SensorRowDto> search(int page, int size, String field, String q) {
        Specification<SensorData> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new java.util.ArrayList<>();
            boolean hasQuery = q != null && !q.isBlank();
            String like = hasQuery ? "%" + q.trim() + "%" : null;
            if ("time".equals(field)) {
                if (hasQuery) ps.add(cb.like(cb.function("DATE_FORMAT", String.class, root.get("createdAt"),
                        cb.literal("%d/%m/%Y %H:%i:%s")), like));
            } else {
                if (field != null && !field.equals("all")) ps.add(cb.equal(root.get("sensor").get("name"), field));
                if (hasQuery) ps.add(cb.like(root.get("value").as(String.class), like));
            }
            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return PageDto.of(repo.findAll(spec, PageRequest.of(Math.max(page, 1) - 1, size, Sort.by("id").descending())),
                SensorRowDto::of);
    }
}
