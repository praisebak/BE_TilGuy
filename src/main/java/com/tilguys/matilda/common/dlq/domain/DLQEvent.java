package com.tilguys.matilda.common.dlq.domain;

import com.tilguys.matilda.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dlq_events")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class DLQEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_event_type", nullable = false)
    private String originalEventType;

    @Column(name = "original_event_id")
    private Long originalEventId;

    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DLQEventStatus status;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retry_count", nullable = false)
    @Builder.Default
    private Integer maxRetryCount = 3;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "alarm_sent", nullable = false)
    @Builder.Default
    private Boolean alarmSent = false;

    public void markAsRetrying() {
        this.status = DLQEventStatus.RETRYING;
        this.lastRetryAt = LocalDateTime.now();
        this.retryCount++;
    }

    public void markAsResolved() {
        this.status = DLQEventStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markAsPermanentlyFailed() {
        this.status = DLQEventStatus.PERMANENTLY_FAILED;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markAlarmSent() {
        this.alarmSent = true;
    }

    public boolean canRetry() {
        return this.retryCount < this.maxRetryCount && 
               this.status == DLQEventStatus.FAILED;
    }

    public boolean shouldSendAlarm() {
        return !this.alarmSent && 
               (this.status == DLQEventStatus.PERMANENTLY_FAILED || 
                this.retryCount >= this.maxRetryCount);
    }
}
