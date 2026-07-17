package com.white.handdam.poll.entity;

import com.white.handdam.global.entity.BaseCreatedAtEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "poll_option",
        uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "order_index"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PollOption extends BaseCreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "option_text", nullable = false, length = 200)
    private String optionText;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public static PollOption create(Long pollId, String optionText, int orderIndex) {
        PollOption o = new PollOption();
        o.pollId = pollId;
        o.optionText = optionText;
        o.orderIndex = orderIndex;
        return o;
    }
}
