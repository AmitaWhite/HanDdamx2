package com.white.handdam.poll.service;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.poll.dto.request.PollCreateRequest;
import com.white.handdam.poll.dto.response.PollResponse;
import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.entity.PollOption;
import com.white.handdam.poll.entity.PollVote;
import com.white.handdam.poll.exception.PollErrorCode;
import com.white.handdam.poll.repository.PollOptionRepository;
import com.white.handdam.poll.repository.PollRepository;
import com.white.handdam.poll.repository.PollVoteRepository;
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
    private final PollVoteRepository pollVoteRepository;

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

    // [LYJ-023] 투표 선택지 + 내 참여 조회
    public PollResponse getPoll(Long pollId, Long memberId) {

        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new CustomException(PollErrorCode.POLL_NOT_FOUND));

        Feed feed = feedRepository.findByIdAndDeletedFalse(poll.getFeedId())
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        // TODO: Project 엔티티 추가 후 creatorId → isOwner / level 확인
        boolean isOwner = false;
        String level = null;
        if (!canAccess(feed.getVisibility(), level, isOwner)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        List<PollOption> options = pollOptionRepository.findByPollIdOrderByOrderIndex(pollId);

        Long myVotedOptionId = null;
        if (memberId != null) {
            myVotedOptionId = pollVoteRepository.findByPollIdAndMemberId(pollId, memberId)
                    .map(vote -> vote.getPollOptionId())
                    .orElse(null);
        }

        return PollResponse.from(poll, options, myVotedOptionId);
    }

    private boolean canAccess(Visibility visibility, String level, boolean isOwner) {
        if (isOwner) return true;
        return switch (visibility) {
            case PUBLIC          -> true;
            case FREE_SUBSCRIBER -> "FREE".equals(level) || "PAID".equals(level);
            case PAID_SUBSCRIBER -> "PAID".equals(level);
        };
    }
}
