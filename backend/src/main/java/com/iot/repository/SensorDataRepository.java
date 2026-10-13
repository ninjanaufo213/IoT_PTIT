package com.iot.repository;

import com.iot.entity.SensorData;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SensorDataRepository extends JpaRepository<SensorData, Long>, JpaSpecificationExecutor<SensorData> {
    Optional<SensorData> findFirstBySensorIdOrderByIdDesc(Long sensorId);
}
