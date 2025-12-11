package com.tilguys.matilda.tag.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tilguys.matilda.common.auth.exception.MatildaException;
import com.tilguys.matilda.tag.domain.TilTagRelations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
public class RecentTilTagsCache {

    private static final String RECENT_TAG_RELATIONS_KEY = "recent:til:relations";

    private final org.springframework.cache.Cache globalCache;
    private final Cache<String, TilTagRelations> localCache;

    public RecentTilTagsCache(CacheManager cacheManager) {
        org.springframework.cache.Cache resolved = cacheManager.getCache("tilTags");
        if (resolved == null) {
            throw new MatildaException("tilTags 캐시를 찾을 수 없습니다");
        }
        this.globalCache = resolved;
        this.localCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(100)
                .build();
    }

    public TilTagRelations getRecentTagRelations() {
        // 1차: 로컬 메모리 캐시(Caffeine, TTL 적용)
        TilTagRelations local = localCache.getIfPresent(RECENT_TAG_RELATIONS_KEY);
        if (local != null) {
            return local;
        }

        try {
            // 2차: 글로벌 캐시(예: Redis)
            TilTagRelations cached = globalCache.get(RECENT_TAG_RELATIONS_KEY, TilTagRelations.class);
            if (cached == null) {
                throw new MatildaException("캐시를 가져오는데 실패하였습니다");
            }
            // 글로벌 캐시 적중 시 로컬도 갱신
            localCache.put(RECENT_TAG_RELATIONS_KEY, cached);
            return cached;
        } catch (Exception e) {
            log.error("최근 태그 정보를 가져오는데 실패하였습니다", e);
            return localCache.getIfPresent(RECENT_TAG_RELATIONS_KEY);
        }
    }

    public void updateRecentTagRelations(TilTagRelations recentTagRelations) {
        // 원본 업데이트 후 두 캐시에 저장
        localCache.put(RECENT_TAG_RELATIONS_KEY, recentTagRelations);
        globalCache.put(RECENT_TAG_RELATIONS_KEY, recentTagRelations);
    }
}
