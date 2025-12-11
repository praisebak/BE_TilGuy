package com.tilguys.matilda.tag.cache;

import com.tilguys.matilda.tag.domain.TilTagRelations;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Caffeine per-entry TTL에 지터 적용.
 */
public class JitterExpiry implements com.github.benmanes.caffeine.cache.Expiry<String, TilTagRelations> {

    private static final long BASE_LOCAL_TTL_SECONDS = 360; // 로컬 캐시 기본 TTL(6분)
    private static final int MIN_TTL_JITTER_SECONDS = 2;    // TTL 지연 최소
    private static final int MAX_TTL_JITTER_SECONDS = 5;    // TTL 지연 최대

    @Override
    public long expireAfterCreate(String key, TilTagRelations value, long currentTime) {
        return jitterNanos();
    }

    @Override
    public long expireAfterUpdate(String key, TilTagRelations value, long currentTime, long currentDuration) {
        return jitterNanos();
    }

    @Override
    public long expireAfterRead(String key, TilTagRelations value, long currentTime, long currentDuration) {
        return currentDuration;
    }

    private long jitterNanos() {
        long jitterSec = ThreadLocalRandom.current()
                .nextLong(MIN_TTL_JITTER_SECONDS, MAX_TTL_JITTER_SECONDS + 1);
        long ttlSec = BASE_LOCAL_TTL_SECONDS + jitterSec;
        return Duration.ofSeconds(ttlSec)
                .toNanos();
    }
}
