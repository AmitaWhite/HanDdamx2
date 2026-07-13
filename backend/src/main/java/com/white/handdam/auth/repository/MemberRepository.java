package com.white.handdam.auth.repository;

import com.white.handdam.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member,Long> {

    boolean existsByEmail(String email);

}
