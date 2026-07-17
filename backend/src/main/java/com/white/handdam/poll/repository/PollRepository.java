package com.white.handdam.poll.repository;

import com.white.handdam.poll.entity.Poll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRepository extends JpaRepository<Poll, Long> {
    boolean existsByFeedId(Long feedId);
}