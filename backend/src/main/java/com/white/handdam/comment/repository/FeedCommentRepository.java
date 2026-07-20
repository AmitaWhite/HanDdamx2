package com.white.handdam.comment.repository;

import com.white.handdam.comment.entity.FeedComment;
import com.white.handdam.member.dto.response.MyFeedCommentResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {
    // [LYJ-015] 특정 피드의 삭제되지 않은 댓글 전체 — 생성일 오름차순
    List<FeedComment> findByFeedIdAndDeletedFalseOrderByCreatedAtAsc(Long feedId);

    // [LYJ-017]
    Optional<FeedComment> findByIdAndDeletedFalse(Long id);

    // [LYJ-019]
     List<FeedComment> findByParentCommentIdAndDeletedFalse(Long parentCommentId);

     // KSY-020 - 내가 작성한 삭제되지 앟은 피드 댓글 - 생성일 최신순 (DTO Projection)
    @Query("""
            select new com.white.handdam.member.dto.response.MyFeedCommentResponse(
                        fc.id, fc.content, f.id, f.title, fc.createdAt
                        )
                from FeedComment fc
                join Feed f on f.id = fc.feedId
                where fc.memberId = :memberId
                and fc.deleted = false
                and f.deleted = false
                order by fc.createdAt desc, fc.id desc
            """)
    Slice<MyFeedCommentResponse> findMyComments(@Param("memberId") Long memberId, Pageable pageable);

}
