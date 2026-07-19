package com.white.handdam.member.service;

import com.white.handdam.board.repository.BoardCommentRepository;
import com.white.handdam.comment.repository.FeedCommentRepository;
import com.white.handdam.member.dto.response.MyBoardCommentResponse;
import com.white.handdam.member.dto.response.MyFeedCommentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MyActivityService {

    private final FeedCommentRepository feedCommentRepository;
    private final BoardCommentRepository boardCommentRepository;

    // KSY-020
    public Slice<MyFeedCommentResponse> getMyFeedComments(Long memberId, Pageable pageable) {
        return feedCommentRepository.findMyComments(memberId, pageable);
    }

    // KSY-021
    public Slice<MyBoardCommentResponse> getMyBoardComments(Long memberId, Pageable pageable) {
        return boardCommentRepository.findMyComments(memberId, pageable);
    }

}
