package com.white.handdam.like.repository;

import com.white.handdam.like.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    boolean existsByFeedIdAndMemberId(Long feedId, Long memberId);

    Optional<FeedLike> findByFeedIdAndMemberId(Long feedId, Long memberId);
}
