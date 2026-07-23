package com.white.handdam.feed.repository;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface FeedRepository extends JpaRepository<Feed, Long> {
    Optional<Feed> findByIdAndDeletedFalse(Long id);

    // 댓글 수 원자적 증감 — JPA dirty-checking 방식(read-modify-write)의 동시성 유실 방지
    @Modifying
    @Query("UPDATE Feed f SET f.commentCount = f.commentCount + 1 WHERE f.id = :feedId")
    void increaseCommentCount(@Param("feedId") Long feedId);

    @Modifying
    @Query("UPDATE Feed f SET f.commentCount = CASE WHEN f.commentCount > 0 THEN f.commentCount - 1 ELSE 0 END WHERE f.id = :feedId")
    void decreaseCommentCount(@Param("feedId") Long feedId);
    // [LYJ-006]
    Slice<Feed> findByVisibilityAndDeletedFalse(Visibility visibility, Pageable pageable);

    // [LYJ-007]
    @Query("""
            SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
            WHERE p.id = f.projectId
              AND f.deleted = false
              AND p.deleted = false
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (
                    p.creatorId = :memberId
                    OR p.creatorId IN :paidCreatorIds
                    OR (p.creatorId IN :freeCreatorIds AND f.visibility IN :freeVisibilities)
              )
            ORDER BY f.createdAt DESC
            """)
    Slice<Feed> findHomeFeeds(
            @Param("memberId")         Long memberId,
            @Param("paidCreatorIds")   List<Long> paidCreatorIds,
            @Param("freeCreatorIds")   List<Long> freeCreatorIds,
            @Param("categoryId")       Long categoryId,
            @Param("freeVisibilities") List<Visibility> freeVisibilities,
            Pageable pageable
    );

    // [LYJ-008]
    @Query("""
            SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
            WHERE p.id = f.projectId
              AND f.deleted = false
              AND p.deleted = false
              AND f.visibility = com.white.handdam.feed.entity.Visibility.PUBLIC
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
            ORDER BY f.createdAt DESC
            """)
    Slice<Feed> findExploreFeeds(
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    // [LYJ-009] 특정 크리에이터의 피드를 조회 - 구독 레벨에 따라 볼 수 있는 공개범위 다름
    @Query("""
            SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
            WHERE p.id = f.projectId
              AND f.deleted = false
              AND p.deleted = false
              AND p.creatorId = :creatorId
              AND f.visibility IN :visibilities
            ORDER BY f.createdAt DESC
            """)
    Slice<Feed> findByCreatorIdAndVisibilityIn(
            @Param("creatorId")    Long creatorId,
            @Param("visibilities") List<Visibility> visibilities,
            Pageable pageable
    );

    // [LYJ-010]
    @Query("""
            SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
            WHERE p.id = f.projectId
              AND f.deleted = false
              AND p.deleted = false
              AND p.creatorId = :creatorId
            ORDER BY f.createdAt DESC
            """)
    Slice<Feed> findByCreatorId(
            @Param("creatorId") Long creatorId,
            Pageable pageable
    );

    // 프로젝트 삭제 전 피드 수 검증
    long countByProjectIdAndDeletedFalse(Long projectId);

    // 프로젝트에 포함된 피드 목록 조회
    Slice<Feed> findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(Long projectId, Pageable pageable);

    // 프로젝트 피드 목록
    Slice<Feed> findByProjectIdAndVisibilityInAndDeletedFalseOrderByCreatedAtDesc(
            Long projectId, List<Visibility> visibility, Pageable pageable);

    // 프로젝트 목록 피드 수 배치 조회
    @Query("SELECT f.projectId, COUNT(f) FROM Feed f WHERE f.projectId IN :projectIds AND f.deleted = false GROUP BY f.projectId")
    List<Object[]> countByProjectIdInAndDeletedFalse(@Param("projectIds") List<Long> projectIds);
}
