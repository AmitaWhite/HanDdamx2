package com.white.handdam.feed.repository;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {
    Optional<Feed> findByIdAndDeletedFalse(Long id);
    // [LYJ-006]
    Slice<Feed> findByVisibilityAndDeletedFalse(Visibility visibility, Pageable pageable);

    // TODO [LYJ-007] project 엔티티 추가 후 사용 - 구독 등급에 따라 다름
    // @Query("""
    //         SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
    //         WHERE p.id = f.projectId
    //           AND f.deleted = false
    //           AND p.deleted = false
    //           AND (:categoryId IS NULL OR p.categoryId = :categoryId)
    //           AND (
    //                 p.creatorId = :memberId
    //                 OR p.creatorId IN :paidCreatorIds
    //                 OR (p.creatorId IN :freeCreatorIds AND f.visibility IN :freeVisibilities)
    //           )
    //         ORDER BY f.createdAt DESC
    //         """)
    // Slice<Feed> findHomeFeeds(
    //         @Param("memberId")         Long memberId,
    //         @Param("paidCreatorIds")   List<Long> paidCreatorIds,
    //         @Param("freeCreatorIds")   List<Long> freeCreatorIds,
    //         @Param("categoryId")       Long categoryId,
    //         @Param("freeVisibilities") List<Visibility> freeVisibilities,
    //         Pageable pageable
    // );

    // TODO [LYJ-008] Project 엔티티 추가 후 사용 (카테고리 필터 포함) - PUBLIC만
    // @Query("""
    //         SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
    //         WHERE p.id = f.projectId
    //           AND f.deleted = false
    //           AND p.deleted = false
    //           AND f.visibility = 'PUBLIC'
    //           AND (:categoryId IS NULL OR p.categoryId = :categoryId)
    //         ORDER BY f.createdAt DESC
    //         """)
    // Slice<Feed> findExploreFeeds(
    //         @Param("categoryId") Long categoryId,
    //         Pageable pageable
    // );

    // TODO [LYJ-009] Project 엔티티 추가 후 사용
    // 특정 크리에이터의 피드를 조회 - 구독 레벨에 따라 볼 수 있는 공개범위 다름
    // @Query("""
    //         SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
    //         WHERE p.id = f.projectId
    //           AND f.deleted = false
    //           AND p.deleted = false
    //           AND p.creatorId = :creatorId
    //           AND f.visibility IN :visibilities
    //         ORDER BY f.createdAt DESC
    //         """)
    // Slice<Feed> findByCreatorIdAndVisibilityIn(
    //         @Param("creatorId")    Long creatorId,
    //         @Param("visibilities") List<Visibility> visibilities,
    //         Pageable pageable
    // );

    // TODO [LYJ-010] Project 엔티티 추가 후 사용
    // @Query("""
    //         SELECT f FROM Feed f, com.white.handdam.project.entity.Project p
    //         WHERE p.id = f.projectId
    //           AND f.deleted = false
    //           AND p.deleted = false
    //           AND p.creatorId = :creatorId
    //         ORDER BY f.createdAt DESC
    //         """)
    // Slice<Feed> findByCreatorId(
    //         @Param("creatorId") Long creatorId,
    //         Pageable pageable
    // );

}