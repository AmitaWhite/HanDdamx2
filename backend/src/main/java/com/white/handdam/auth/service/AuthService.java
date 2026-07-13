package com.white.handdam.auth.service;

import com.white.handdam.auth.repository.MemberRepository;
import com.white.handdam.auth.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;

    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmail(EmailNormalizer.normalize(email));
    }

}
