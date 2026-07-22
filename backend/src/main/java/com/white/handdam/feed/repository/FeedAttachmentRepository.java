package com.white.handdam.feed.repository;

import com.white.handdam.feed.entity.FeedAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface FeedAttachmentRepository extends JpaRepository<FeedAttachment, Long> {
    // feedId + attachmentId 동시 검증
    Optional<FeedAttachment> findByIdAndFeedIdAndDeletedFalse(Long id, Long feedId);

    // 피드 첨부파일 목록 — 삭제되지 않은 것만, 등록 순서(orderIndex)대로
    List<FeedAttachment> findByFeedIdAndDeletedFalseOrderByOrderIndex(Long feedId);
}
