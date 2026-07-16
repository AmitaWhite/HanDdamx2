package com.white.handdam.auth.oauth;

import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.OAuthProvider;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {
        // 1. 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Attribute 추출
        String email = oAuth2User.getAttribute("email");
        String oauthId = oAuth2User.getAttribute("sub"); // Google이 제공하는 고유 사용자 식별값
        String name =  oAuth2User.getAttribute("name");
        String profileImageUrl = oAuth2User.getAttribute("picture");

        // 3. Google 계졍으로 가입한 회원인지 확인
        Member member = memberRepository.findByOauthProviderAndOauthId(
                OAuthProvider.GOOGLE,
                oauthId
        ).orElseGet(() -> registerNewMember( // 4. 기존 회원 없으면 신규 회원 생성
                email,
                oauthId,
                name,
                profileImageUrl
        ));

        return new CustomOAuth2User(member, oAuth2User.getAttributes());

    }

    private Member registerNewMember(String email, String oAuthId, String name, String profileImageUrl) {
        memberRepository.findByEmail(email).ifPresent(existing -> {
            throw new CustomException(OAuthErrorCode.OAUTH_EMAIL_ALREADY_EXISTS);
        });

        Member member = Member.createOAuthMember(email, name, profileImageUrl, OAuthProvider.GOOGLE, oAuthId);
        return memberRepository.save(member);
    }

}
