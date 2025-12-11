package com.tilguys.matilda.common.dlq.domain;

import com.tilguys.matilda.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "alarm_sent", nullable = false)
    @Builder.Default
    private Boolean alarmSent = false;

    public void markAlarmSent() {
        this.alarmSent = true;
    }

    public boolean shouldSendAlarm() {
        return !this.alarmSent && this.status == DLQEventStatus.PERMANENTLY_FAILED;
    }
}
