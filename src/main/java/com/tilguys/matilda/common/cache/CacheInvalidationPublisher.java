package com.tilguys.matilda.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 캐시 무효화 메시지를 Redis Pub/Sub으로 전파.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationPublisher {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${matilda.cache.invalidation.topic:cache:invalidate}")
    private String topic;

    /**
     * 무효화할 캐시 키들을 브로드캐스트.
     */
    public void publish(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        String payload = String.join(",", keys);
        stringRedisTemplate.convertAndSend(topic, payload);
        log.info("Published cache invalidation for keys={} on topic={}", payload, topic);
    }
}
