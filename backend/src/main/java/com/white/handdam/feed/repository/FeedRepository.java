package com.white.handdam.feed.repository;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeedRepository extends JpaRepository<Feed, Long> {
    Optional<Feed> findByIdAndDeletedFalse(Long id);
    // [LYJ-006]
    Page<Feed> findByVisibilityAndDeletedFalse(Visibility visibility, Pageable pageable);
}