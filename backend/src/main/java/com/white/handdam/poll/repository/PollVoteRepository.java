package com.white.handdam.poll.repository;

import com.white.handdam.poll.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    // [LYJ-023]
    Optional<PollVote> findByPollIdAndMemberId(Long pollId, Long memberId);

    // [LYJ-025]
    void deleteByPollId(Long pollId);

    // [LYJ-026]
    boolean existsByPollIdAndMemberId(Long pollId, Long memberId);

}
