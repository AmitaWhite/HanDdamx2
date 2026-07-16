package com.white.handdam.comment.repository;

import com.white.handdam.comment.entity.FeedComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedCommentRepository extends JpaRepository<FeedComment, Long> {
    // [LYJ-015] 특정 피드의 삭제되지 않은 댓글 전체 — 생성일 오름차순
    List<FeedComment> findByFeedIdAndDeletedFalseOrderByCreatedAtAsc(Long feedId);
}
