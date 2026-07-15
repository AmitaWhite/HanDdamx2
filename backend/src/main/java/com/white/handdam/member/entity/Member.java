package com.white.handdam.member.entity;

import com.white.handdam.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String password; // OAuth 가입자는 NULL

    @Column(nullable = false, unique = true, length = 255)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "profile_image_storage_key", length = 500)
    private String profileImageStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 50)
    private OAuthProvider oauthProvider = OAuthProvider.NONE;

    @Column(name = "oauth_id", length = 255)
    private String oauthId;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt; // NULL = 미인증

    @Builder(access = AccessLevel.PRIVATE)
    private Member(String email, String password, String nickname, Role role, OAuthProvider oauthProvider, String oauthId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
    }

    public static Member createLocalMember(String email, String encodedPassword, String nickname) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .role(Role.USER)
                .oauthProvider(OAuthProvider.NONE)
                .build();
    }

    public static Member createSocialMember(String email, String nickname,
                                            OAuthProvider oauthProvider, String oauthId) {
        return Member.builder()
                .email(email)
                .nickname(nickname)
                .role(Role.USER)
                .oauthProvider(oauthProvider)
                .oauthId(oauthId)
                .build();
    }

    public void markEmailVerified() {
        this.emailVerifiedAt = Instant.now();
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeRoleToCreator() { this.role = Role.CREATOR; }
}
