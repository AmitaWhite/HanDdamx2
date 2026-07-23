package com.white.handdam.creator.repository;

import com.white.handdam.creator.entity.CreatorProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 크리에이터 프로필 Repository.
 */
public interface CreatorProfileRepository extends JpaRepository<CreatorProfile, Long> {

    /** member_id로 프로필 단건 조회 */
    Optional<CreatorProfile> findByMemberId(Long memberId);
}