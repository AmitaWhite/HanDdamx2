package com.white.handdam.auth.service;

import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.auth.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    // KSY-001
    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmail(EmailNormalizer.normalize(email));
    }

    // KSY-002
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNickname(nickname);
    }

}
