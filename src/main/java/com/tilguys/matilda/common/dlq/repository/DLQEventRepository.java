package com.tilguys.matilda.common.dlq.repository;

import com.tilguys.matilda.common.dlq.domain.DLQEvent;
import com.tilguys.matilda.common.dlq.domain.DLQEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DLQEventRepository extends JpaRepository<DLQEvent, Long> {


    /**
     * 알람을 보내야 하는 이벤트 조회
     */
    @Query("SELECT d FROM DLQEvent d WHERE d.alarmSent = false AND d.status = 'PERMANENTLY_FAILED'")
    List<DLQEvent> findEventsNeedingAlarm();

    @Query("SELECT d FROM DLQEvent d WHERE d.status = 'RESOLVED' AND d.resolvedAt < :cutoffDate")
    List<DLQEvent> findOldResolvedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);

}
