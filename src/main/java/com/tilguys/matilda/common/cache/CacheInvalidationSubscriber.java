package com.tilguys.matilda.common.cache;

import com.tilguys.matilda.tag.cache.RecentTilTagsCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {

    private final RecentTilTagsCacheService recentTilTagsCacheService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        if (payload.isEmpty()) {
            return;
        }
        List<String> keys = Arrays.stream(payload.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (keys.isEmpty()) {
            return;
        }
        applyJitterBackoff();
        log.info("Received cache invalidation keys={}", keys);
        recentTilTagsCacheService.invalidate(keys);
    }

    /**
     * 무효화 폭주 분산용 지연 (2~5초).
     */
    private void applyJitterBackoff() {
        long delayMs = ThreadLocalRandom.current().nextLong(2000, 5001);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
