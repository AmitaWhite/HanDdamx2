package com.white.handdam.notification.repository;

import com.white.handdam.notification.entity.NotificationEntity;
import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 파생 쿼리 검증과 함께, {@code @CreatedDate} 가 실제 영속성 컨텍스트에서
 * {@code created_at} 을 채우는지 확인한다 (단위 테스트로는 증명할 수 없는 부분).
 */
@SpringBootTest(properties = {
	"jwt.secret=test_secret_for_notification_repository_1234567890"
})
@ActiveProfiles("test")
@Transactional
class NotificationRepositoryTest {

	private static final Long MEMBER_ID = 1L;
	private static final Long OTHER_MEMBER_ID = 9L;
	private static final Long SENDER_ID = 2L;
	private static final Long CHAT_ROOM_ID = 42L;

	@Autowired
	private NotificationRepository notificationRepository;

	@MockitoBean
	private ClientRegistrationRepository clientRegistrationRepository;

	@Test
	@DisplayName("저장 시 createdAt이 JPA Auditing으로 자동 채워진다")
	void createdAtIsPopulatedByAuditing() {
		NotificationEntity saved = notificationRepository.saveAndFlush(notification(MEMBER_ID));

		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	@DisplayName("내 알림 목록은 최신순으로 반환되고 다른 회원의 알림은 제외된다")
	void findByMemberIdOrderByCreatedAtDesc() {
		NotificationEntity first = notificationRepository.saveAndFlush(notification(MEMBER_ID));
		NotificationEntity second = notificationRepository.saveAndFlush(notification(MEMBER_ID));
		notificationRepository.saveAndFlush(notification(OTHER_MEMBER_ID));

		Slice<NotificationEntity> result = notificationRepository
			.findByMemberIdOrderByCreatedAtDesc(MEMBER_ID, PageRequest.of(0, 10));

		assertThat(result.getContent())
			.extracting(NotificationEntity::getMemberId)
			.containsOnly(MEMBER_ID);
		assertThat(result.getContent())
			.extracting(NotificationEntity::getId)
			.containsExactlyInAnyOrder(first.getId(), second.getId());
		assertThat(result.getContent())
			.isSortedAccordingTo((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
	}

	@Test
	@DisplayName("페이지 크기를 넘으면 hasNext가 true다")
	void findByMemberIdOrderByCreatedAtDesc_hasNext() {
		notificationRepository.saveAll(
			List.of(notification(MEMBER_ID), notification(MEMBER_ID), notification(MEMBER_ID)));
		notificationRepository.flush();

		Slice<NotificationEntity> result = notificationRepository
			.findByMemberIdOrderByCreatedAtDesc(MEMBER_ID, PageRequest.of(0, 2));

		assertThat(result.getContent()).hasSize(2);
		assertThat(result.hasNext()).isTrue();
	}

	@Test
	@DisplayName("안 읽은 알림 개수는 읽은 알림과 다른 회원의 알림을 제외한다")
	void countByMemberIdAndIsReadFalse() {
		notificationRepository.saveAndFlush(notification(MEMBER_ID));
		notificationRepository.saveAndFlush(notification(MEMBER_ID));

		NotificationEntity read = notification(MEMBER_ID);
		read.markAsRead();
		notificationRepository.saveAndFlush(read);

		notificationRepository.saveAndFlush(notification(OTHER_MEMBER_ID));

		assertThat(notificationRepository.countByMemberIdAndIsReadFalse(MEMBER_ID)).isEqualTo(2L);
	}

	private NotificationEntity notification(Long memberId) {
		return NotificationEntity.builder()
			.memberId(memberId)
			.senderId(SENDER_ID)
			.type(NotificationType.CHAT_MESSAGE)
			.message("안녕하세요")
			.referenceId(CHAT_ROOM_ID)
			.referenceType("CHAT_ROOM")
			.build();
	}
}
