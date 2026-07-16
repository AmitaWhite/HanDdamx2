package com.white.handdam.comment.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "feed_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedComment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // depth=0 이면 null, depth=1 이면 부모 댓글 id
    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    // 0: 댓글, 1: 대댓글 — DB CHECK 제약으로 두 값만 허용
    @Column(nullable = false)
    private short depth;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static FeedComment create(Long feedId, Long memberId, Long parentCommentId, short depth, String content) {
        FeedComment c = new FeedComment();
        c.feedId = feedId;
        c.memberId = memberId;
        c.parentCommentId = parentCommentId;
        c.depth = depth;
        c.content = content;
        return c;
    }
}