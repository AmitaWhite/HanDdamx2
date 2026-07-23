package com.white.handdam.search.repository;

import com.white.handdam.feed.entity.Feed;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 피드 검색 전용 읽기 리포지토리.
 * 공유 도메인 리포지토리(FeedRepository)를 상속하지 않고 Spring Data 마커 인터페이스만
 * 상속하여 검색 쿼리 한 개만 노출한다(쓰기 메서드 미상속 = 순수 읽기).
 */
public interface FeedSearchRepository extends Repository<Feed, Long> {

    /**
     * 둘러보기 탐색 조건(PUBLIC + 미삭제 + 카테고리 필터)에 keyword(제목/본문) 조건을 추가한 검색.
     * FeedRepository.findExploreFeeds 의 가시성/보안 규칙을 그대로 따른다.
     * keyword 가 null 이면 explore 와 동일하게 동작한다.
     */
    @Query("""
            SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
            WHERE p.id = f.projectId
              AND f.deleted = false
              AND p.deleted = false
              AND f.visibility = com.white.handdam.feed.entity.Visibility.PUBLIC
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (
                    :keyword IS NULL
                    OR LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(f.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY f.createdAt DESC
            """)
    Slice<Feed> findSearchFeeds(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}
