package com.tilguys.matilda.common.dlq.service;

import com.tilguys.matilda.common.dlq.domain.DLQEvent;
import com.tilguys.matilda.common.dlq.domain.DLQEventStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class DLQAlarmService {

    private static final Logger log = LoggerFactory.getLogger(DLQAlarmService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate = new RestTemplate();
    private final String slackWebhookUrl;
    private final String environment;

    public DLQAlarmService(
            @Value("${slack.webhook.url:}") String slackWebhookUrl,
            @Value("${spring.profiles.active:local}") String environment
    ) {
        this.slackWebhookUrl = slackWebhookUrl;
        this.environment = environment;
    }

    /**
     * DLQ 이벤트에 대한 Slack 알람 전송
     */
    public void sendDLQAlarm(DLQEvent dlqEvent) {
        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            log.warn("Slack webhook URL not configured, skipping alarm");
            return;
        }

        try {
            String message = buildSlackMessage(dlqEvent);
            sendSlackMessage(message);
            log.info("DLQ alarm sent for event {}", dlqEvent.getId());
        } catch (Exception e) {
            log.error("Failed to send DLQ alarm for event {}", dlqEvent.getId(), e);
        }
    }


    private String buildSlackMessage(DLQEvent dlqEvent) {
        StringBuilder message = new StringBuilder();

        // 헤더
        message.append("🚨 *DLQ Alert - ")
                .append(environment.toUpperCase())
                .append("*\n\n");

        // 기본 정보
        message.append("*Event Details:*\n");
        message.append("• ID: `")
                .append(dlqEvent.getId())
                .append("`\n");
        message.append("• Type: `")
                .append(dlqEvent.getOriginalEventType())
                .append("`\n");
        message.append("• Status: `")
                .append(dlqEvent.getStatus())
                .append("`\n");
        message.append("• Created: `")
                .append(dlqEvent.getCreatedAt()
                        .format(FORMATTER))
                .append("`\n\n");

        // 에러 정보
        if (dlqEvent.getErrorMessage() != null) {
            message.append("*Error Message:*\n");
            message.append("```")
                    .append(truncateText(dlqEvent.getErrorMessage(), 500))
                    .append("```\n\n");
        }

        // 스택 트레이스 (영구 실패인 경우만)
        if (dlqEvent.getStatus() == DLQEventStatus.PERMANENTLY_FAILED && dlqEvent.getStackTrace() != null) {
            message.append("*Stack Trace:*\n");
            message.append("```")
                    .append(truncateText(dlqEvent.getStackTrace(), 1000))
                    .append("```\n\n");
        }

        // 페이로드 (마지막 200자만)
        if (dlqEvent.getPayload() != null) {
            message.append("*Payload (last 200 chars):*\n");
            message.append("```")
                    .append(truncateText(dlqEvent.getPayload(), 200))
                    .append("```\n\n");
        }

        // 액션 제안
        message.append("*Suggested Actions:*\n");
        message.append("• Check external service status\n");
        message.append("• Review error logs\n");
        message.append("• Consider manual intervention\n");

        return message.toString();
    }


    private void sendSlackMessage(String message) {
        Map<String, Object> payload = Map.of(
                "text", message,
                "username", "Matilda-DLQ-Bot",
                "icon_emoji", ":warning:"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(slackWebhookUrl, request, String.class);
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
