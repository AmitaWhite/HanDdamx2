package com.white.handdam.notification.repository;

import com.white.handdam.notification.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

	Slice<NotificationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

	long countByMemberIdAndIsReadFalse(Long memberId);

	/**
	 * 본인 알림 전체 읽음 처리. 이미 읽은 알림은 건드리지 않는다.
	 *
	 * <p>{@code memberId} 조건이 쿼리에 있어 본인 알림만 갱신되므로 별도 소유권 검사가 필요 없다.
	 *
	 * <p>{@code clearAutomatically = true} 인 이유: 벌크 업데이트는 영속성 컨텍스트를 우회하므로,
	 * 같은 트랜잭션에서 이미 로드된 엔티티가 갱신 전 {@code isRead} 값을 들고 있게 된다.
	 *
	 * @return 실제로 읽음 처리된 건수
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE NotificationEntity n
		SET n.isRead = true
		WHERE n.memberId = :memberId
		  AND n.isRead = false
		""")
	int markAllAsRead(@Param("memberId") Long memberId);
}
