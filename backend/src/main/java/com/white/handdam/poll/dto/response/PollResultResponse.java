package com.white.handdam.poll.dto.response;

import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.entity.PollOption;
import com.white.handdam.poll.entity.PollVote;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record PollResultResponse(
    Long pollId,
    String question,
    boolean active,
    int totalWeight,
    List<OptionResult> options
) {
    public record OptionResult(
        Long optionId,
        String optionText,
        int orderIndex,
        int weight
    ) {}

    public static PollResultResponse from(Poll poll, List<PollOption> options, List<PollVote> votes) {
        Map<Long, Integer> weightByOption = votes.stream()
            .collect(Collectors.groupingBy(
                PollVote::getPollOptionId,
                Collectors.summingInt(v -> (int) v.getWeight())
            ));

        int totalWeight = votes.stream().mapToInt(v -> (int) v.getWeight()).sum();

        List<OptionResult> optionResults = options.stream()
            .map(o -> new OptionResult(
                o.getId(),
                o.getOptionText(),
                o.getOrderIndex(),
                weightByOption.getOrDefault(o.getId(), 0)
            ))
            .toList();

        return new PollResultResponse(poll.getId(), poll.getQuestion(),
            poll.isActive(), totalWeight, optionResults);
    }
}
