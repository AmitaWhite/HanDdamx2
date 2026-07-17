package com.white.handdam.poll.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "poll")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Poll extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "feed_id", nullable = false, unique = true)
    private Long feedId;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false;

    public static Poll create(Long feedId, String question, Instant endAt) {
        Poll poll = new Poll();
        poll.feedId = feedId;
        poll.question = question;
        poll.endAt = endAt;
        return poll;
    }

    public boolean isActive(){
        return !closed && Instant.now().isBefore(endAt);
    }
}
