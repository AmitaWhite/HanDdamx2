package com.white.handdam.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionPaidSubscriptionCheckerTest {

	@Mock
	private SubscriptionRepository subscriptionRepository;

	@InjectMocks
	private SubscriptionPaidSubscriptionChecker checker;

	@Test
	@DisplayName("ACTIVE PAID 구독이면 true")
	void activePaidReturnsTrue() {
		Subscription paid = Subscription.createPaid(99L, 1L, 9900, Instant.parse("2026-07-01T00:00:00Z"));
		given(subscriptionRepository.findBySubscriberIdAndCreatorId(99L, 1L))
			.willReturn(Optional.of(paid));

		assertThat(checker.hasActivePaidSubscription(99L, 1L)).isTrue();
	}

	@Test
	@DisplayName("CANCEL_SCHEDULED PAID 구독이면 true")
	void cancelScheduledPaidReturnsTrue() {
		Subscription paid = Subscription.createPaid(99L, 1L, 9900, Instant.parse("2026-07-01T00:00:00Z"));
		paid.scheduleCancellation(Instant.parse("2026-07-10T00:00:00Z"));
		given(subscriptionRepository.findBySubscriberIdAndCreatorId(99L, 1L))
			.willReturn(Optional.of(paid));

		assertThat(checker.hasActivePaidSubscription(99L, 1L)).isTrue();
	}

	@Test
	@DisplayName("FREE 구독이면 false")
	void freeReturnsFalse() {
		Subscription free = Subscription.createFree(99L, 1L, Instant.parse("2026-07-01T00:00:00Z"));
		given(subscriptionRepository.findBySubscriberIdAndCreatorId(99L, 1L))
			.willReturn(Optional.of(free));

		assertThat(checker.hasActivePaidSubscription(99L, 1L)).isFalse();
	}

	@Test
	@DisplayName("구독이 없으면 false")
	void missingReturnsFalse() {
		given(subscriptionRepository.findBySubscriberIdAndCreatorId(99L, 1L))
			.willReturn(Optional.empty());

		assertThat(checker.hasActivePaidSubscription(99L, 1L)).isFalse();
	}

	@Test
	@DisplayName("memberId가 null이면 false")
	void nullMemberReturnsFalse() {
		assertThat(checker.hasActivePaidSubscription(null, 1L)).isFalse();
	}
}
