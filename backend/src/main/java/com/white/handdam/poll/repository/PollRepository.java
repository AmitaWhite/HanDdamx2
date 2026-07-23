package com.white.handdam.poll.repository;

import com.white.handdam.poll.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    boolean existsByFeedId(Long feedId);
    Optional<Poll> findByFeedId(Long feedId);
}