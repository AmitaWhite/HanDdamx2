package com.white.handdam.like.service;

import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.like.dto.response.FeedLikeResponse;
import com.white.handdam.like.entity.FeedLike;
import com.white.handdam.like.exception.FeedLikeErrorCode;
import com.white.handdam.like.repository.FeedLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedLikeService {
    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;

    // [LYJ-020] 좋아요 등록
    @Transactional
    public FeedLikeResponse addLike(Long feedId, Long memberId){
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        if (feedLikeRepository.existsByFeedIdAndMemberId(feedId, memberId)){
            throw new CustomException(FeedLikeErrorCode.ALREADY_LIKED);
        }

        feedLikeRepository.save(FeedLike.create(feedId, memberId));
        feed.increaseLikeCount();
        return new FeedLikeResponse(feed.getLikeCount(), true);
    }

    // [LYJ-021] 좋아요 취소
    @Transactional
    public FeedLikeResponse cancelLike(Long feedId, Long memberId){
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        FeedLike like = feedLikeRepository.findByFeedIdAndMemberId(feedId, memberId)
                .orElseThrow(() -> new CustomException(FeedLikeErrorCode.NOT_LIKED));

        feedLikeRepository.delete(like);
        feed.decreaseLikeCount();
        return new FeedLikeResponse(feed.getLikeCount(), false);
    }
}
