package com.white.handdam.like.repository;

import com.white.handdam.like.entity.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    boolean existsByFeedIdAndMemberId(Long feedId, Long memberId);

    Optional<FeedLike> findByFeedIdAndMemberId(Long feedId, Long memberId);

    // 목록 조회 시 로그인 사용자가 좋아요한 피드 id만 배치로 조회 (N+1 방지)
    List<FeedLike> findByMemberIdAndFeedIdIn(Long memberId, Collection<Long> feedIds);
}
