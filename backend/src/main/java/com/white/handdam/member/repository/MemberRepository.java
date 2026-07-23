package com.white.handdam.member.repository;

import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member,Long> {

    // KSY-001
    boolean existsByEmail(String email);

    // KSY-002
    boolean existsByNickname(String nickname);

    // KSY-003
    Optional<Member> findByEmail(String email);

    // KSY-010
    Optional<Member> findByOauthProviderAndOauthId(OAuthProvider oauthProvider, String oauthId);
}
