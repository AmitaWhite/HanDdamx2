package com.white.handdam.feed.entity;

import com.white.handdam.storage.StoredObject;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

// [LYJ-012]
@Entity
@Table(name = "feed_attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FeedAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AttachmentType type;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static FeedAttachment ofUpload(Long feedId, AttachmentType type,
                                          StoredObject stored, long fileSize, String mimeType) {
        FeedAttachment a = new FeedAttachment();
        a.feedId = feedId;
        a.type = type;
        a.url = stored.url();
        a.storageKey = stored.storageKey();
        a.originalName = stored.originalName();
        a.fileSize = fileSize;
        a.mimeType = mimeType;
        return a;
    }

    // 외부 영상 링크 (VIDEO_LINK)
    public static FeedAttachment ofVideoLink(Long feedId, String externalUrl) {
        FeedAttachment a = new FeedAttachment();
        a.feedId = feedId;
        a.type = AttachmentType.VIDEO_LINK;
        a.url = externalUrl;
        return a;
    }

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }
}
