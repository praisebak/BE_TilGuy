package com.tilguys.matilda.tag.service;

import com.tilguys.matilda.tag.domain.TagRelation;
import com.tilguys.matilda.tag.repository.TagRelationRepository;
import com.tilguys.matilda.til.domain.Tag;
import com.tilguys.matilda.til.domain.Til;
import com.tilguys.matilda.til.service.TilService;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;

@Service
@Slf4j
public class TagRelationService {

    private static final Long TAG_RELATION_RENEW_PERIOD = 500L;
    private static final int BATCH_SAVE_SIZE = 1000;
    private static final String TAG_RELATION_LOCK_KEY = "TAG_RELATION_LOCK";

    private final TagRelationRepository tagRelationRepository;
    private final RedisLockRegistry redisLockRegistry;
    private final TilService tilService;
    private final EntityManager entityManager;

    public TagRelationService(
            TagRelationRepository tagRelationRepository,
            RedisLockRegistry redisLockRegistry,
            TilService tilService,
            EntityManager entityManager
    ) {
        this.tagRelationRepository = tagRelationRepository;
        this.redisLockRegistry = redisLockRegistry;
        this.tilService = tilService;
        this.entityManager = entityManager;
    }

    @Transactional
    public void renewCoreTagsRelation() {
        Lock lock = redisLockRegistry.obtain(TAG_RELATION_LOCK_KEY);
        boolean acquired = lock.tryLock();
        if (!acquired) {
            log.info("renewCoreTagsRelation skipped: lock already held");
            return;
        }

        try {
            tagRelationRepository.deleteAllInBatch();

            LocalDateTime startDateTime = LocalDate.now()
                    .minusDays(TAG_RELATION_RENEW_PERIOD)
                    .atStartOfDay();

            List<Til> recentWroteTil = tilService.getRecentWroteTil(startDateTime);

            List<TagRelation> tilRelations = new ArrayList<>();

            int count = 0;
            for (Til til : recentWroteTil) {
                List<Tag> tags = til.getTags();
                tilRelations.addAll(createRelatedTag(tags));

                for (TagRelation relation : tilRelations) {
                    entityManager.persist(relation);
                    count++;

                    if (count % BATCH_SAVE_SIZE == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                }
                tilRelations.clear();
            }

            entityManager.flush();
            entityManager.clear();
        } finally {
            lock.unlock();
        }
    }


    private List<TagRelation> createRelatedTag(List<Tag> tags) {
        List<TagRelation> tagRelations = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            for (int j = 0; j < tags.size(); j++) {
                if (i == j) {
                    continue;
                }
                tagRelations.add(new TagRelation(null, tags.get(i), tags.get(j)));
            }
        }
        return tagRelations;
    }

    @Transactional(readOnly = true)
    public Map<Tag, List<Tag>> getRecentRelationTagMap() {
        LocalDateTime startDateTime = LocalDate.now()
                .minusDays(TAG_RELATION_RENEW_PERIOD)
                .atStartOfDay();
        List<TagRelation> tagRelations = tagRelationRepository.findByCreatedAtGreaterThanEqual(startDateTime);

        Map<Tag, List<Tag>> tagMap = new HashMap<>();
        for (TagRelation tagRelation : tagRelations) {
            if (!tagRelation.getTag()
                    .getTil()
                    .isNotDeleted()) {
                continue;
            }

            List<Tag> relationTags = tagMap.getOrDefault(tagRelation.getTag(), new ArrayList<>());
            relationTags.add(tagRelation.getOtherTag());
            tagMap.put(tagRelation.getTag(), relationTags);
        }
        return tagMap;
    }
}
