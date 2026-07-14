package com.white.handdam.auth.service;

import com.white.handdam.auth.dto.request.SignupRequest;
import com.white.handdam.auth.dto.response.SignupResponse;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthService authService;

    // KSY-001
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

    // KSY-002
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

    // KSY-003
    @Test
    @DisplayName("회원가입 성공 시 회원 저장 및 인증 이벤트 발행")
    void signupSuccess() {
        // given
        SignupRequest request = new SignupRequest(
                "test@handdam.com",
                "Passw0rd!23",
                "테스트닉네임"
        );

        // 이메일 중복X 설정
        given(memberRepository.existsByEmail(request.email()))
                .willReturn(false);
        // 닉네임 중복X 설정
        given(memberRepository.existsByNickname(request.nickname()))
                .willReturn(false);
        given(passwordEncoder.encode(request.password()))
                .willReturn("encodedPassword");
        given(memberRepository.save(any(Member.class)))
                .willAnswer(invocation -> invocation.getArgument(0)); // Member 객체 반환

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.email())
                .isEqualTo(request.email());

        // Member 저장 확인
        then(memberRepository)
                .should()
                .save(any(Member.class));

        then(emailVerificationService)
                .should()
                .issueAndSend(any(), eq(request.email()), eq(request.nickname()), eq(VerificationPurpose.SIGNUP));
    }

    @Test
    @DisplayName("이메일 중복 시 예외를 던지고 저장 X")
    void signupFailWhenEmailDuplicated() {
        // given
        SignupRequest request = new SignupRequest(
                "test@handdam.com",
                "Passw0rd!23",
                "테스트닉네임"
        );
        given(memberRepository.existsByEmail(request.email()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.DUPLICATE_EMAIL);

        then(memberRepository).should(never()).save(any(Member.class));
        then(emailVerificationService).should(never())
                .issueAndSend(any(), any(), any(), any());
    }

    @Test
    @DisplayName("닉네임 중복 시 예외를 던지고 저장 X")
    void signupFailWhenNicknameDuplicated() {
        // given
        SignupRequest request = new SignupRequest(
                "test@handdam.com",
                "Passw0rd!23",
                "테스트닉네임"
        );
        given(memberRepository.existsByEmail(request.email()))
                .willReturn(false);
        given(memberRepository.existsByNickname(request.nickname()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.DUPLICATE_NICKNAME);

        then(memberRepository).should(never()).save(any(Member.class));
        then(emailVerificationService).should(never())
                .issueAndSend(any(), any(), any(), any());
    }

}