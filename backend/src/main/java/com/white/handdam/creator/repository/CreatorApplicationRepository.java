package com.white.handdam.creator.repository;

import com.white.handdam.creator.entity.CreatorApplication;
import com.white.handdam.creator.entity.CreatorApplicationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 크리에이터 전환 신청 Repository.
 * - 신청 이력은 삭제하지 않음
 */
public interface CreatorApplicationRepository extends JpaRepository<CreatorApplication, Long> {

    /**
     * PENDING 신청 중복 여부 확인.
     */
    boolean existsByMemberIdAndStatus(Long memberId, CreatorApplicationStatus status);

    /**
     * 내 최근 신청 1건 조회
     */
    Optional<CreatorApplication> findFirstByMemberIdOrderByAppliedAtDesc(Long memberId);

    /**
     * 내 전체 신청 이력 조회
     */
    List<CreatorApplication> findByMemberIdOrderByAppliedAtDesc(Long memberId);

    /**
     * 관리자용 전체 신청 목록.
     * status가 null이면 전체 조회, 값이 있으면 해당 상태만 필터
     */
    @Query("""
            SELECT a FROM CreatorApplication a
            WHERE (:status IS NULL OR a.status = :status)
            ORDER BY a.appliedAt DESC
            """)
    Page<CreatorApplication> findAllByStatus(
            @Param("status") CreatorApplicationStatus status,
            Pageable pageable
    );
}