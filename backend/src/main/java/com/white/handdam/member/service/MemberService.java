package com.white.handdam.member.service;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.dto.request.MemberUpdateRequest;
import com.white.handdam.member.dto.response.MemberProfileResponse;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.exception.MemberErrorCode;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    // 단건 조회
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // 존재 여부 확인
    public boolean existsById(Long memberId) {
        return memberRepository.existsById(memberId);
    }

    // KSY-014
    public MemberProfileResponse getMyProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
        return MemberProfileResponse.from(member);
    }

    // KSY-016
    @Transactional
    public MemberProfileResponse updateMyProfile(Long memberId, MemberUpdateRequest request) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (!member.getNickname().equals(request.nickname())
            && memberRepository.existsByNickname(request.nickname())) {
            throw new CustomException(MemberErrorCode.DUPLICATE_NICKNAME);
        }

        member.updateNickname(request.nickname());
        return MemberProfileResponse.from(member);
    }

}
