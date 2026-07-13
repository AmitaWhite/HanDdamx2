package com.white.handdam.auth.repository;

import com.white.handdam.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member,Long> {

    // KSY-001
    boolean existsByEmail(String email);

    // KSY-002
    boolean existsByNickname(String nickname);
}
