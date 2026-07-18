package com.white.handdam.poll.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "poll_vote",
        uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "member_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "poll_option_id", nullable = false)
    private Long pollOptionId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private short weight;

    @Column(name = "subscription_level_snapshot", nullable = false, length = 50)
    private String subscriptionLevelSnapshot;

    public static PollVote create(Long pollId, Long pollOptionId, Long memberId,
                                  short weight, String subscriptionLevelSnapshot) {
        PollVote v = new PollVote();
        v.pollId = pollId;
        v.pollOptionId = pollOptionId;
        v.memberId = memberId;
        v.weight = weight;
        v.subscriptionLevelSnapshot = subscriptionLevelSnapshot;
        return v;
    }

}
