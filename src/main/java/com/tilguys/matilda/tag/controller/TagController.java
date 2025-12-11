package com.tilguys.matilda.tag.controller;

import com.tilguys.matilda.tag.cache.RecentTilTagsCacheService;
import com.tilguys.matilda.tag.domain.TilTagRelations;
import com.tilguys.matilda.tag.service.RecentTilTagsProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags")
public class TagController {

    private final RecentTilTagsCacheService recentTilTagsCacheService;
    private final RecentTilTagsProvider recentTilTagsProvider;

    @GetMapping("/recent")
    public ResponseEntity<TilTagRelations> getRecentTags() {
        LocalTime start = LocalTime.now();
        TilTagRelations recentTagRelations = recentTilTagsCacheService.getRecentTagRelations(
                recentTilTagsProvider::load
        );
        log.debug("{}초 소요됨",
                LocalTime.now()
                        .toSecondOfDay() - start.toSecondOfDay()
        );
        return ResponseEntity.ok(recentTagRelations);
    }
}
