package com.white.handdam.search.controller;

import com.white.handdam.feed.dto.response.FeedSummaryResponse;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.SliceResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.search.dto.response.CreatorSearchResponse;
import com.white.handdam.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통합 검색 API. 비로그인 접근 가능(SecurityConfig PERMIT_ALL).
 * 작품/크리에이터를 별도 엔드포인트로 분리해 프론트에서 병렬 호출한다.
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /** GET /api/search/feeds — 작품(제목/본문) 검색 + 카테고리 필터, 무한스크롤. */
    @GetMapping("/feeds")
    public ApiResponse<SliceResponse<FeedSummaryResponse>> searchFeeds(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @AuthenticationPrincipal AuthMember member,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Long memberId = member != null ? member.id() : null;
        return ApiResponse.success(
                SliceResponse.from(searchService.searchFeeds(memberId, keyword, categoryId, pageable)));
    }

    /** GET /api/search/creators — 크리에이터(작가명) 검색. */
    @GetMapping("/creators")
    public ApiResponse<SliceResponse<CreatorSearchResponse>> searchCreators(
            @RequestParam String keyword,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        return ApiResponse.success(
                SliceResponse.from(searchService.searchCreators(keyword, pageable)));
    }
}
