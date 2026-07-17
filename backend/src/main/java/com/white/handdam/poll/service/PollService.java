package com.white.handdam.poll.service;

import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.poll.dto.request.PollCreateRequest;
import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.entity.PollOption;
import com.white.handdam.poll.exception.PollErrorCode;
import com.white.handdam.poll.repository.PollOptionRepository;
import com.white.handdam.poll.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PollService {

    private final FeedRepository feedRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;

    // [LYJ-022] 기존 피드에 투표 추가
    @Transactional
    public Long createPoll(Long feedId, Long memberId, PollCreateRequest request) {
        feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        // TODO: Project 엔티티 추가 후 사용
        // if (!project.getCreatorId().equals(memberId)) {
        //     throw new CustomException(PollErrorCode.POLL_FORBIDDEN);
        // }

        if (pollRepository.existsByFeedId(feedId)) {
            throw new CustomException(PollErrorCode.POLL_ALREADY_EXISTS);
        }

        Poll poll = Poll.create(feedId, request.question(), request.endAt());
        pollRepository.save(poll);

        List<PollOption> options = new ArrayList<>();
        for (int i = 0; i < request.options().size(); i++) {
            String optionText = request.options().get(i);
            PollOption option = PollOption.create(poll.getId(), optionText, i);
            options.add(option);
        }
        pollOptionRepository.saveAll(options);

        return poll.getId();
    }
}
