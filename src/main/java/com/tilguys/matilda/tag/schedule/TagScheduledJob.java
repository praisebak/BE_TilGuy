package com.tilguys.matilda.tag.schedule;


import com.tilguys.matilda.common.cache.CacheInvalidationPublisher;
import com.tilguys.matilda.tag.cache.RecentTilTagsCacheService;
import com.tilguys.matilda.tag.domain.TilTagRelations;
import com.tilguys.matilda.tag.service.RecentTilTagsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(
        name = "matilda.cache.tag.enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class TagScheduledJob {

    private final RecentTilTagsProvider recentTilTagsProvider;
    private final RecentTilTagsCacheService recentTilTagsCacheService;
    private final CacheInvalidationPublisher cacheInvalidationPublisher;

    public TagScheduledJob(
            RecentTilTagsProvider recentTilTagsProvider,
            RecentTilTagsCacheService recentTilTagsCacheService,
            CacheInvalidationPublisher cacheInvalidationPublisher
    ) {
        this.recentTilTagsProvider = recentTilTagsProvider;
        this.recentTilTagsCacheService = recentTilTagsCacheService;
        this.cacheInvalidationPublisher = cacheInvalidationPublisher;
    }

    @Scheduled(cron = "0 */30 * * * *")
    public void updateRecentTagRelations() {
        log.info("recent tag 관계 캐싱 시작!");
        TilTagRelations recentTagRelations = recentTilTagsProvider.load();
        recentTilTagsCacheService.updateRecentTagRelations(recentTagRelations);
        cacheInvalidationPublisher.publish(java.util.List.of("recent:til:relations"));
        log.info("recent tag 관계 캐싱 완료");
        
    }
}
