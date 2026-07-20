package com.white.handdam.feed.dto.response;

import com.white.handdam.feed.entity.AttachmentType;
import com.white.handdam.feed.entity.FeedAttachment;

import java.time.Instant;

// [LYJ-012] 첨부파일 응답
public record AttachmentResponse(
    Long id,
    Long feedId,
    AttachmentType type,
    String url,
    String originalName,
    Long fileSize,
    String mimeType,
    int orderIndex,
    Instant createdAt
) {
    public static AttachmentResponse from(FeedAttachment a) {
        return new AttachmentResponse(
            a.getId(), a.getFeedId(), a.getType(),
            a.getUrl(), a.getOriginalName(),
            a.getFileSize(), a.getMimeType(),
            a.getOrderIndex(), a.getCreatedAt()
        );
    }
}
