package com.white.handdam.feed.repository;

import com.white.handdam.feed.entity.FeedAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface FeedAttachmentRepository extends JpaRepository<FeedAttachment, Long> {
    // feedId + attachmentId 동시 검증
    Optional<FeedAttachment> findByIdAndFeedIdAndDeletedFalse(Long id, Long feedId);
}
