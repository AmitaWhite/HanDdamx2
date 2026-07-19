package com.white.handdam.feed.dto.response;

import com.white.handdam.feed.entity.AttachmentType;
import com.white.handdam.feed.entity.FeedAttachment;

// [LYJ-014] 첨부파일 다운로드 응답
// IMAGE/FILE → presignedUrl(10분 유효), VIDEO_LINK → 외부 url 그대로
public record DownloadResponse(
    Long attachmentId,
    AttachmentType type,
    String downloadUrl,
    String originalName,
    Integer expiresInMinutes
) {
    // IMAGE / FILE 타입: Presigned URL
    public static DownloadResponse ofPresigned(FeedAttachment a, String presignedUrl, int expireMinutes) {
        return new DownloadResponse(
            a.getId(), a.getType(), presignedUrl, a.getOriginalName(), expireMinutes
        );
    }

    // VIDEO_LINK 타입: 외부 URL 그대로
    public static DownloadResponse ofLink(FeedAttachment a) {
        return new DownloadResponse(a.getId(), a.getType(), a.getUrl(), null, null);
    }
}
