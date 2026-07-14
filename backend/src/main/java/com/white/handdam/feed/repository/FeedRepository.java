package com.white.handdam.feed.repository;

import com.white.handdam.feed.entity.Feed;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {
    Optional<Feed> findByIdAndDeletedFalse(Long id);
}