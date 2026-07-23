package com.white.handdam.like.entity;

import com.white.handdam.global.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "feed_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"feed_id", "member_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike extends BaseCreatedAtEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false)
    private Long feedId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    public static FeedLike create(Long feedId, Long memberId){
        FeedLike like = new FeedLike();
        like.feedId = feedId;
        like.memberId = memberId;

        return like;
    }
}
