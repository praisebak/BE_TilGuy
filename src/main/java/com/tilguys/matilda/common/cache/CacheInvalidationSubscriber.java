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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {

    private static final long MAX_JITTER_MS = 500L;

    private final RecentTilTagsCacheService recentTilTagsCacheService;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

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

        applyJitterBackoff(keys);
    }

    private void applyJitterBackoff(List<String> keys) {
        long delayMs = ThreadLocalRandom.current()
                .nextLong(0, MAX_JITTER_MS);

        scheduler.schedule(
                () -> {
                    recentTilTagsCacheService.invalidate(keys);
                },
                delayMs,
                TimeUnit.MILLISECONDS
        );
    }
}
