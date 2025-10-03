package com.tilguys.matilda.common.dlq.service;

import com.tilguys.matilda.common.dlq.config.DLQConfiguration;
import com.tilguys.matilda.common.dlq.domain.DLQEvent;
import com.tilguys.matilda.common.dlq.domain.DLQEventStatus;
import com.tilguys.matilda.common.dlq.repository.DLQEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DLQService {

    private static final Logger log = LoggerFactory.getLogger(DLQService.class);

    private final DLQEventRepository dlqEventRepository;
    private final DLQAlarmService dlqAlarmService;
    private final DLQConfiguration dlqConfiguration;

    public DLQService(DLQEventRepository dlqEventRepository, 
                     DLQAlarmService dlqAlarmService,
                     DLQConfiguration dlqConfiguration) {
        this.dlqEventRepository = dlqEventRepository;
        this.dlqAlarmService = dlqAlarmService;
        this.dlqConfiguration = dlqConfiguration;
    }

    /**
     * 실패한 이벤트를 DLQ에 저장
     */
    @Transactional
    public void sendToDLQ(String originalEventType, Long originalEventId, 
                         String payload, String errorMessage, String stackTrace) {
        
        DLQEvent dlqEvent = DLQEvent.builder()
                .originalEventType(originalEventType)
                .originalEventId(originalEventId)
                .payload(payload)
                .errorMessage(errorMessage)
                .stackTrace(stackTrace)
                .status(DLQEventStatus.FAILED)
                .maxRetryCount(dlqConfiguration.getMaxRetryCount())
                .build();

        dlqEventRepository.save(dlqEvent);
        
        log.warn("Event sent to DLQ: {} (ID: {})", originalEventType, dlqEvent.getId());
        
        // 즉시 알람 시도 (비동기)
        sendAlarmIfNeeded(dlqEvent.getId());
    }

    /**
     * DLQ 이벤트 재처리 시도
     */
    @Transactional
    public void retryDLQEvent(Long dlqEventId) {
        DLQEvent dlqEvent = dlqEventRepository.findById(dlqEventId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: " + dlqEventId));

        if (!dlqEvent.canRetry()) {
            log.warn("DLQ event {} cannot be retried (retry count: {}/{})", 
                    dlqEventId, dlqEvent.getRetryCount(), dlqEvent.getMaxRetryCount());
            return;
        }

        dlqEvent.markAsRetrying();
        dlqEventRepository.save(dlqEvent);

        log.info("Retrying DLQ event {} (attempt {}/{})", 
                dlqEventId, dlqEvent.getRetryCount(), dlqEvent.getMaxRetryCount());

        // 실제 재처리 로직은 각 서비스에서 구현
        // 여기서는 상태만 업데이트
    }

    /**
     * DLQ 이벤트를 성공으로 마킹
     */
    @Transactional
    public void markAsResolved(Long dlqEventId) {
        DLQEvent dlqEvent = dlqEventRepository.findById(dlqEventId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: " + dlqEventId));

        dlqEvent.markAsResolved();
        dlqEventRepository.save(dlqEvent);

        log.info("DLQ event {} marked as resolved", dlqEventId);
    }

    /**
     * DLQ 이벤트를 영구 실패로 마킹
     */
    @Transactional
    public void markAsPermanentlyFailed(Long dlqEventId) {
        DLQEvent dlqEvent = dlqEventRepository.findById(dlqEventId)
                .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: " + dlqEventId));

        dlqEvent.markAsPermanentlyFailed();
        dlqEventRepository.save(dlqEvent);

        log.error("DLQ event {} marked as permanently failed", dlqEventId);
        
        // 영구 실패 시 알람 전송
        sendAlarmIfNeeded(dlqEventId);
    }

    /**
     * 주기적으로 재시도 가능한 이벤트들 처리
     */
    @Scheduled(fixedDelay = 300000) // 5분마다
    @Transactional
    public void processRetryableEvents() {
        List<DLQEvent> retryableEvents = dlqEventRepository.findRetryableEvents(DLQEventStatus.FAILED);

        if (!retryableEvents.isEmpty()) {
            log.info("Processing {} retryable DLQ events", retryableEvents.size());

            for (DLQEvent event : retryableEvents) {
                try {
                    retryDLQEvent(event.getId());
                } catch (Exception e) {
                    log.error("Failed to retry DLQ event {}", event.getId(), e);
                }
            }
        }
    }

    /**
     * 주기적으로 알람이 필요한 이벤트들 처리
     */
    @Scheduled(fixedDelay = 60000) // 1분마다
    @Transactional
    public void processAlarmEvents() {
        List<DLQEvent> alarmEvents = dlqEventRepository.findEventsNeedingAlarm();

        if (!alarmEvents.isEmpty()) {
            log.info("Processing {} DLQ events needing alarm", alarmEvents.size());

            for (DLQEvent event : alarmEvents) {
                try {
                    sendAlarmIfNeeded(event.getId());
                } catch (Exception e) {
                    log.error("Failed to send alarm for DLQ event {}", event.getId(), e);
                }
            }
        }
    }

    /**
     * 오래된 해결된 이벤트들 정리
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(dlqConfiguration.getCleanupDays());
        List<DLQEvent> oldEvents = dlqEventRepository.findOldResolvedEvents(cutoffDate);

        if (!oldEvents.isEmpty()) {
            log.info("Cleaning up {} old resolved DLQ events", oldEvents.size());
            dlqEventRepository.deleteAll(oldEvents);
        }
    }

    /**
     * 비동기로 알람 전송
     */
    @Async
    public void sendAlarmIfNeeded(Long dlqEventId) {
        try {
            DLQEvent dlqEvent = dlqEventRepository.findById(dlqEventId)
                    .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: " + dlqEventId));

            if (dlqEvent.shouldSendAlarm()) {
                dlqAlarmService.sendDLQAlarm(dlqEvent);
                dlqEvent.markAlarmSent();
                dlqEventRepository.save(dlqEvent);
            }
        } catch (Exception e) {
            log.error("Failed to send alarm for DLQ event {}", dlqEventId, e);
        }
    }

}
