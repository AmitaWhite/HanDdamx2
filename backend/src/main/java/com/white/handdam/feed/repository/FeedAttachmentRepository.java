package com.white.handdam.feed.repository;

import com.white.handdam.feed.entity.FeedAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface FeedAttachmentRepository extends JpaRepository<FeedAttachment, Long> {
    // feedId + attachmentId 동시 검증
    Optional<FeedAttachment> findByIdAndFeedIdAndDeletedFalse(Long id, Long feedId);

    // 피드 첨부파일 목록 — 삭제되지 않은 것만, 등록 순서(orderIndex)대로
    List<FeedAttachment> findByFeedIdAndDeletedFalseOrderByOrderIndex(Long feedId);

    // 목록 조회 시 대표 썸네일 계산용 — 여러 피드의 첨부파일을 배치로 조회 (N+1 방지)
    List<FeedAttachment> findByFeedIdInAndDeletedFalseOrderByOrderIndex(Collection<Long> feedIds);
}
