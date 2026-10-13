package com.iot.repository;

import com.iot.entity.ActionHistory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActionHistoryRepository extends JpaRepository<ActionHistory, Long>, JpaSpecificationExecutor<ActionHistory> {
    List<ActionHistory> findByStatusOrderByIdAsc(String status);

    List<ActionHistory> findByStatusAndCreatedAtBefore(String status, LocalDateTime time);
}
