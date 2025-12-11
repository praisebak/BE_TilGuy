package com.tilguys.matilda.tag.service;

import com.tilguys.matilda.tag.domain.SubTag;
import com.tilguys.matilda.tag.domain.TilTagRelations;
import com.tilguys.matilda.til.domain.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 최근 태그/서브태그/관계 데이터를 DB에서 조회해 캐시로 공급하는 프로바이더.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecentTilTagsProvider {

    private static final int TAG_GET_START_DAY = 500;

    private final TagRelationService tagRelationService;
    private final TilTagService tilTagService;

    public TilTagRelations load() {
        // 최신 코어 태그 관계 먼저 재계산
        tagRelationService.renewCoreTagsRelation();

        LocalDate startDay = LocalDate.now().minusDays(TAG_GET_START_DAY);

        List<Tag> tags = tilTagService.getRecentWroteTags(startDay)
                .stream()
                .filter(tag -> tag.getTil().isNotDeleted())
                .toList();

        List<SubTag> subTags = tilTagService.getRecentSubTags(startDay)
                .stream()
                .filter(subTag -> subTag.getTag() != null
                        && subTag.getTag().getTil() != null
                        && subTag.getTag().getTil().isNotDeleted())
                .toList();

        Map<Tag, List<Tag>> tagRelationMap = tagRelationService.getRecentRelationTagMap();
        log.info("created tags size : {} subTags size : {} tagRelationMap size : {} ",
                tags.size(), subTags.size(), tagRelationMap.size());

        return new TilTagRelations(tags, subTags, tagRelationMap);
    }
}
