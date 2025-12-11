package com.tilguys.matilda.tag.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tilguys.matilda.common.auth.exception.MatildaException;
import com.tilguys.matilda.tag.domain.TilTagRelations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service
public class RecentTilTagsCacheService {

    private static final String RECENT_TAG_RELATIONS_KEY = "recent:til:relations";
    // Jitter 설정
    private static final int MIN_DELAY_MS = 2000;   // 최소 지연(ms)
    private static final int MAX_DELAY_MS = 5000;   // 최대 지연(ms)

    private final org.springframework.cache.Cache globalCache;
    private final Cache<String, TilTagRelations> localCache;

    public RecentTilTagsCacheService(CacheManager cacheManager) {
        org.springframework.cache.Cache resolved = cacheManager.getCache("tilTags");
        if (resolved == null) {
            throw new MatildaException("tilTags 캐시를 찾을 수 없습니다");
        }
        
        this.globalCache = resolved;
        this.localCache = Caffeine.newBuilder()
                .expireAfter(new JitterExpiry())
                .maximumSize(100)
                .build();
    }

    /**
     * 로컬 → 글로벌 → 로더(DB 등) 순으로 조회.
     */
    public TilTagRelations getRecentTagRelations(Supplier<TilTagRelations> loader) {
        // 1차: 로컬 메모리 캐시(Caffeine, TTL 적용)
        TilTagRelations local = getRecentTagFromLocal();
        if (local != null) {
            return local;
        }

        // 로컬 미스 → 중앙(글로벌/DB) 조회 전에 확률적 Jitter로 스탬피드 분산
        maybeApplyJitterBackoff();

        try {
            // 2차: 글로벌 캐시(예: Redis)
            TilTagRelations cached = globalCache.get(RECENT_TAG_RELATIONS_KEY, TilTagRelations.class);
            if (cached == null) {
                throw new MatildaException("캐시를 가져오는데 실패하였습니다");
            }
            // 글로벌 캐시 적중 시 TTL 갱신(재저장) + 로컬도 갱신
            globalCache.put(RECENT_TAG_RELATIONS_KEY, cached);
            localCache.put(RECENT_TAG_RELATIONS_KEY, cached);
            return cached;
        } catch (Exception e) {
            log.error("최근 태그 정보를 가져오는데 실패하였습니다", e);
        }

        // 3차: 로더(DB)에서 조회 후 두 캐시에 적재
        maybeApplyJitterBackoff(); // 글로벌 미스 후 DB 호출 전에도 한 번 더 분산
        TilTagRelations loaded = loader.get();
        if (loaded != null) {
            updateRecentTagRelations(loaded);
        }
        return loaded;
    }

    /**
     * (하위 호환) 로더 없이 호출하는 기존 코드 지원.
     * 캐시 미스 시 null 반환 가능.
     */
    public TilTagRelations getRecentTagRelations() {
        return getRecentTagRelations(this::emptyRelations);
    }

    private TilTagRelations getRecentTagFromLocal() {
        return localCache.getIfPresent(RECENT_TAG_RELATIONS_KEY);
    }

    public void updateRecentTagRelations(TilTagRelations recentTagRelations) {
        // 원본 업데이트 후 두 캐시에 저장
        localCache.put(RECENT_TAG_RELATIONS_KEY, recentTagRelations);
        globalCache.put(RECENT_TAG_RELATIONS_KEY, recentTagRelations);
    }

    /**
     * 캐시 무효화 (로컬/글로벌).
     */
    public void invalidate(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            localCache.invalidate(key);
        }
    }

    private TilTagRelations emptyRelations() {
        return new TilTagRelations(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyMap()
        );
    }

    /**
     * 캐시 스탬피드 완화를 위한 확률적 무작위 대기.
     */
    private void maybeApplyJitterBackoff() {
        long delayMs = ThreadLocalRandom.current()
                .nextLong(MIN_DELAY_MS, MAX_DELAY_MS + 1); // [min, max] 포함
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread()
                    .interrupt();
        }
    }
}
