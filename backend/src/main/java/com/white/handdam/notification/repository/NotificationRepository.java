package com.white.handdam.notification.repository;

import com.white.handdam.notification.entity.NotificationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

	Slice<NotificationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

	long countByMemberIdAndIsReadFalse(Long memberId);
}
