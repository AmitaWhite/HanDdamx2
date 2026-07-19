package com.white.handdam.poll.dto.response;

import com.white.handdam.poll.entity.PollOption;

public record PollOptionResponse(
        Long optionId,
        String optionText,
        int orderIndex
) {
    public static PollOptionResponse from(PollOption option) {
        return new PollOptionResponse(
                option.getId(),
                option.getOptionText(),
                option.getOrderIndex()
        );
    }
}