package com.white.handdam.poll.dto.response;

import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.entity.PollOption;

import java.time.Instant;
import java.util.List;

public record PollResponse(
        Long pollId,
        Long feedId,
        String question,
        Instant endAt,
        boolean closed,
        boolean active,
        List<PollOptionResponse> options,
        Long myVotedOptionId
) {
    public static PollResponse from(Poll poll, List<PollOption> options, Long myVotedOptionId) {
        return new PollResponse(
                poll.getId(),
                poll.getFeedId(),
                poll.getQuestion(),
                poll.getEndAt(),
                poll.isClosed(),
                poll.isActive(),
                options.stream().map(option -> PollOptionResponse.from(option)).toList(),
                myVotedOptionId
        );
    }
}
