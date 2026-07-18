package com.white.handdam.poll.service;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.poll.dto.request.PollCreateRequest;
import com.white.handdam.poll.dto.request.PollUpdateRequest;
import com.white.handdam.poll.dto.request.PollVoteRequest;
import com.white.handdam.poll.dto.response.PollResponse;
import com.white.handdam.poll.dto.response.PollResultResponse;
import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.entity.PollOption;
import com.white.handdam.poll.entity.PollVote;
import com.white.handdam.poll.exception.PollErrorCode;
import com.white.handdam.poll.repository.PollOptionRepository;
import com.white.handdam.poll.repository.PollRepository;
import com.white.handdam.poll.repository.PollVoteRepository;
import com.white.handdam.project.entity.Project;
import com.white.handdam.project.exception.ProjectErrorCode;
import com.white.handdam.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final ProjectRepository projectRepository;
    private final SubscriptionLevelChecker subscriptionLevelChecker;

    // [LYJ-022] 기존 피드에 투표 추가 (크리에이터만 가능)
    @Transactional
    public Long createPoll(Long feedId, Long memberId, PollCreateRequest request) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(PollErrorCode.POLL_FORBIDDEN);
        }

        if (pollRepository.existsByFeedId(feedId)) {
            throw new CustomException(PollErrorCode.POLL_ALREADY_EXISTS);
        }

        Poll poll = Poll.create(feedId, request.question(), request.endAt());
        try {
            pollRepository.save(poll);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(PollErrorCode.POLL_ALREADY_EXISTS);
        }

        List<PollOption> options = new ArrayList<>();
        for (int i = 0; i < request.options().size(); i++) {
            options.add(PollOption.create(poll.getId(), request.options().get(i), i));
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

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        boolean isOwner = memberId != null && project.getCreatorId().equals(memberId);
        String level = (memberId != null && !isOwner)
                ? subscriptionLevelChecker.getLevel(memberId, project.getCreatorId())
                : null;
        if (!canAccess(feed.getVisibility(), level, isOwner)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        List<PollOption> options = pollOptionRepository.findByPollIdOrderByOrderIndex(pollId);

        Long myVotedOptionId = null;
        if (memberId != null) {
            myVotedOptionId = pollVoteRepository.findByPollIdAndMemberId(pollId, memberId)
                    .map(PollVote::getPollOptionId)
                    .orElse(null);
        }

        return PollResponse.from(poll, options, myVotedOptionId);
    }

    // [LYJ-024] 투표 질문·종료일 수정
    @Transactional
    public Long updatePoll(Long pollId, Long memberId, PollUpdateRequest request) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_NOT_FOUND));

        Feed feed = feedRepository.findByIdAndDeletedFalse(poll.getFeedId())
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(PollErrorCode.POLL_FORBIDDEN);
        }

        poll.update(request.question(), request.endAt());
        return poll.getId();
    }

    // [LYJ-025] 투표 삭제 (크리에이터 전용)
    @Transactional
    public void deletePoll(Long pollId, Long memberId) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_NOT_FOUND));

        Feed feed = feedRepository.findByIdAndDeletedFalse(poll.getFeedId())
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));

        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(PollErrorCode.POLL_FORBIDDEN);
        }

        pollVoteRepository.deleteByPollId(pollId);
        pollOptionRepository.deleteByPollId(pollId);
        pollRepository.delete(poll);
    }

    // [LYJ-026] 투표 참여
    @Transactional
    public Long vote(Long pollId, Long memberId, PollVoteRequest request) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_NOT_FOUND));

        if (!poll.isActive()) {
            throw new CustomException(PollErrorCode.POLL_CLOSED);
        }

        Feed feed = feedRepository.findByIdAndDeletedFalse(poll.getFeedId())
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));

        boolean isOwner = project.getCreatorId().equals(memberId);
        String level = isOwner ? null
            : subscriptionLevelChecker.getLevel(memberId, project.getCreatorId());

        if (!canAccess(feed.getVisibility(), level, isOwner)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        if (pollVoteRepository.existsByPollIdAndMemberId(pollId, memberId)) {
            throw new CustomException(PollErrorCode.POLL_ALREADY_VOTED);
        }

        PollOption option = pollOptionRepository.findById(request.optionId())
            .filter(o -> o.getPollId().equals(pollId))
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_OPTION_INVALID));

        short weight       = "PAID".equals(level) ? (short) 2 : (short) 1;
        String snapshot    = "PAID".equals(level) ? "PAID" : "FREE";

        PollVote vote = PollVote.create(pollId, option.getId(), memberId, weight, snapshot);
        return pollVoteRepository.save(vote).getId();
    }

    // [LYJ-027] 내 투표 선택지 변경
    @Transactional
    public Long changeVote(Long pollId, Long memberId, PollVoteRequest request) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_NOT_FOUND));

        if (!poll.isActive()) {
            throw new CustomException(PollErrorCode.POLL_CLOSED);
        }

        Feed feed = feedRepository.findByIdAndDeletedFalse(poll.getFeedId())
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));

        boolean isOwner = project.getCreatorId().equals(memberId);
        String level = isOwner ? null
            : subscriptionLevelChecker.getLevel(memberId, project.getCreatorId());

        if (!canAccess(feed.getVisibility(), level, isOwner)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        PollVote existingVote = pollVoteRepository.findByPollIdAndMemberId(pollId, memberId)
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_NOT_VOTED));

        PollOption option = pollOptionRepository.findById(request.optionId())
            .filter(o -> o.getPollId().equals(pollId))
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_OPTION_INVALID));

        existingVote.changeOption(option.getId());
        return existingVote.getId();
    }

    // [LYJ-028] 가중치 반영 투표 결과 조회
    public PollResultResponse getPollResults(Long pollId, Long memberId) {
        Poll poll = pollRepository.findById(pollId)
            .orElseThrow(() -> new CustomException(PollErrorCode.POLL_NOT_FOUND));

        Feed feed = feedRepository.findByIdAndDeletedFalse(poll.getFeedId())
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));

        boolean isOwner = memberId != null && project.getCreatorId().equals(memberId);
        String level = (memberId != null && !isOwner)
            ? subscriptionLevelChecker.getLevel(memberId, project.getCreatorId())
            : null;

        if (!canAccess(feed.getVisibility(), level, isOwner)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        List<PollOption> options = pollOptionRepository.findByPollIdOrderByOrderIndex(pollId);
        List<PollVote> votes = pollVoteRepository.findByPollId(pollId);

        return PollResultResponse.from(poll, options, votes);
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
