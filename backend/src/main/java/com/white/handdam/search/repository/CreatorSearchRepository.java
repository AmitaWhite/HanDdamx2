package com.white.handdam.search.repository;

import com.white.handdam.member.entity.Member;
import com.white.handdam.search.dto.response.CreatorSearchResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * 크리에이터 검색 전용 읽기 리포지토리.
 * 작가명은 Member.nickname 에 있으므로 Member 와 CreatorProfile 을 조인해 검색한다.
 * 공유 도메인 리포지토리를 상속하지 않고 마커 인터페이스만 상속(순수 읽기).
 * JPQL 생성자 표현식으로 CreatorSearchResponse 를 직접 반환한다.
 */
public interface CreatorSearchRepository extends Repository<Member, Long> {

    @Query("""
            SELECT new com.white.handdam.search.dto.response.CreatorSearchResponse(
                    m.id, m.nickname, m.profileImageUrl, p.introduction)
            FROM Member m, com.white.handdam.creator.entity.CreatorProfile p
            WHERE p.memberId = m.id
              AND m.role = com.white.handdam.member.entity.Role.CREATOR
              AND LOWER(m.nickname) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY m.nickname ASC
            """)
    Slice<CreatorSearchResponse> searchCreators(
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
