package com.white.handdam.member.service;

import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 단건 조회
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    // 존재 여부 확인
    public boolean existsById(Long memberId) {
        return memberRepository.existsById(memberId);
    }

}
