package com.white.handdam.feed.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// [LYJ-001] POST /api/feeds
@Entity
@Table(name = "feed")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Visibility visibility;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "comment_count", nullable = false)
    private long commentCount = 0;

    @Column(name = "like_count", nullable = false)
    private long likeCount = 0;

    public static Feed create(Long projectId, String title, String content, Visibility visibility) {
        Feed f = new Feed();
        f.projectId = projectId;
        f.title = title;
        f.content = content;
        f.visibility = visibility;
        return f;
    }
}
