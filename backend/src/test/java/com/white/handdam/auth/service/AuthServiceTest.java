package com.white.handdam.auth.service;

import com.white.handdam.auth.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("존재하지 않는 이메일은 사용 가능")
    void shouldReturnTrueWhenEmailDoesNotExist() {
        given(memberRepository.existsByEmail("new@handdam.com")).willReturn(false);

        boolean available = authService.isEmailAvailable("new@handdam.com");

        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 이메일은 사용 불가")
    void shouldReturnFalseWhenEmailExists() {
        given(memberRepository.existsByEmail("taken@handdam.com")).willReturn(true);

        boolean available = authService.isEmailAvailable("taken@handdam.com");

        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("사용 가능한 닉네임이면 true 반환")
    void shouldReturnTrueWhenNicknameDoesNotExist() {
        given(memberRepository.existsByNickname("newNickname"))
                .willReturn(false);

        boolean available = authService.isNicknameAvailable("newNickname");

        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("이미 존재하는 닉네임이면 false 반환")
    void shouldReturnFalseWhenNicknameExists() {
        given(memberRepository.existsByNickname("takenNickname"))
                .willReturn(true);

        boolean available = authService.isNicknameAvailable("takenNickname");

        assertThat(available).isFalse();
    }
}