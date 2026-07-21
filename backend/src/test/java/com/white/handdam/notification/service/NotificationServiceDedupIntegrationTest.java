package com.white.handdam.notification.service;

import com.white.handdam.notification.entity.NotificationEntity.NotificationType;
import com.white.handdam.notification.entity.NotificationReferenceType;
import com.white.handdam.notification.repository.NotificationRepository;
import com.white.handdam.notification.websocket.publisher.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
	"jwt.secret=test_secret_for_notification_dedup_integration_1234567890",
	"subscription.expiration.enabled=false",
	"subscription.expiring-notification.enabled=false"
})
@ActiveProfiles("test")
class NotificationServiceDedupIntegrationTest {

	private static final Long RECIPIENT_ID = 1L;
	private static final Long SUBSCRIPTION_ID = 10L;
	private static final String DEDUP_KEY = "SUBSCRIPTION_EXPIRING:10:2026-08-13T00:00:00Z";

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private NotificationRepository notificationRepository;

	@MockitoBean
	private ClientRegistrationRepository clientRegistrationRepository;

	@MockitoBean
	private NotificationPublisher notificationPublisher;

	@BeforeEach
	void setUp() {
		notificationRepository.deleteAll();
	}

	@Test
	@DisplayName("same dedupKey is stored only once")
	void sameDedupKeyStoredOnlyOnce() {
		boolean first = create(DEDUP_KEY);
		boolean second = create(DEDUP_KEY);

		assertThat(first).isTrue();
		assertThat(second).isFalse();
		assertThat(notificationRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("different dedupKey values are stored separately")
	void differentDedupKeysAreStoredSeparately() {
		boolean first = create(DEDUP_KEY);
		boolean second = create("SUBSCRIPTION_EXPIRING:10:2026-08-14T00:00:00Z");

		assertThat(first).isTrue();
		assertThat(second).isTrue();
		assertThat(notificationRepository.findAll()).hasSize(2);
	}

	@Test
	@DisplayName("concurrent calls with same dedupKey leave one notification")
	void concurrentSameDedupKeyLeavesOneNotification() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> futures = new ArrayList<>();
		for (int index = 0; index < 2; index++) {
			futures.add(executorService.submit(() -> {
				ready.countDown();
				start.await();
				return create(DEDUP_KEY);
			}));
		}

		assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();
		start.countDown();

		int createdCount = 0;
		for (Future<Boolean> future : futures) {
			if (future.get()) {
				createdCount++;
			}
		}
		executorService.shutdownNow();

		assertThat(createdCount).isEqualTo(1);
		assertThat(notificationRepository.findAll()).hasSize(1);
	}

	private boolean create(String dedupKey) {
		return notificationService.createIfAbsent(
			RECIPIENT_ID,
			null,
			NotificationType.SUBSCRIPTION_EXPIRING,
			"subscription expiring",
			SUBSCRIPTION_ID,
			NotificationReferenceType.SUBSCRIPTION,
			dedupKey
		);
	}
}
