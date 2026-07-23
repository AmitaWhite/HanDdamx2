package com.white.handdam.search.service;

import com.white.handdam.feed.dto.response.FeedSummaryResponse;
import com.white.handdam.feed.service.FeedService;
import com.white.handdam.search.dto.response.CreatorSearchResponse;
import com.white.handdam.search.repository.CreatorSearchRepository;
import com.white.handdam.search.repository.FeedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 통합 검색 서비스. 작품(Feed)·크리에이터(Member) 검색을 담당한다.
 * 피드 요약 매핑은 FeedService 의 N+1 방지 매퍼(toSummarySlice)를 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final FeedSearchRepository feedSearchRepository;
    private final CreatorSearchRepository creatorSearchRepository;
    private final FeedService feedService;

    /** 작품(Feed) 검색 — 제목/본문 keyword + 카테고리 필터. keyword 가 비면 explore 와 동일. */
    public Slice<FeedSummaryResponse> searchFeeds(Long memberId, String keyword, Long categoryId, Pageable pageable) {
        return feedService.toSummarySlice(
                feedSearchRepository.findSearchFeeds(normalize(keyword), categoryId, pageable),
                memberId
        );
    }

    /** 크리에이터(작가명) 검색. */
    public Slice<CreatorSearchResponse> searchCreators(String keyword, Pageable pageable) {
        return creatorSearchRepository.searchCreators(normalize(keyword), pageable);
    }

    /** 공백/빈 문자열은 null 로 정규화(LIKE 조건 우회). */
    private static String normalize(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    }
}
