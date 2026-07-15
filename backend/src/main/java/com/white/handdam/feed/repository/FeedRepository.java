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

    // TODO [LYJ-007] project 엔티티 추가 후 아래 주석 해제 후 사용
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
}