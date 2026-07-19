package com.white.handdam.member.service;

import com.white.handdam.comment.repository.FeedCommentRepository;
import com.white.handdam.member.dto.response.MyFeedCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyActivityService {

    private final FeedCommentRepository feedCommentRepository;

    // KSY-020
    public Slice<MyFeedCommentResponse> getMyFeedComments(Long memberId, Pageable pageable) {
        return feedCommentRepository.findMyComments(memberId, pageable);
    }

}
