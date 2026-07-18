package com.white.handdam.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
	@DisplayName("기간 내 ACTIVE PAID 구독이면 true")
	void activePaidWithinPeriodReturnsTrue() {
		Subscription paid = Subscription.createPaid(
			99L, 1L, 9900, Instant.now().minus(5, ChronoUnit.DAYS)
		);
		given(subscriptionRepository.findBySubscriberIdAndCreatorId(99L, 1L))
			.willReturn(Optional.of(paid));

		assertThat(checker.hasActivePaidSubscription(99L, 1L)).isTrue();
	}

	@Test
	@DisplayName("기간 내 CANCEL_SCHEDULED PAID 구독이면 true")
	void cancelScheduledPaidWithinPeriodReturnsTrue() {
		Subscription paid = Subscription.createPaid(
			99L, 1L, 9900, Instant.now().minus(5, ChronoUnit.DAYS)
		);
		paid.scheduleCancellation(Instant.now());
		given(subscriptionRepository.findBySubscriberIdAndCreatorId(99L, 1L))
			.willReturn(Optional.of(paid));

		assertThat(checker.hasActivePaidSubscription(99L, 1L)).isTrue();
	}

	@Test
	@DisplayName("유료 기간이 끝나면 false")
	void expiredPaidReturnsFalse() {
		Subscription paid = Subscription.createPaid(
			99L, 1L, 9900, Instant.now().minus(40, ChronoUnit.DAYS)
		);
		given(subscriptionRepository.findBySubscriberIdAndCreatorId(99L, 1L))
			.willReturn(Optional.of(paid));

		assertThat(checker.hasActivePaidSubscription(99L, 1L)).isFalse();
	}

	@Test
	@DisplayName("FREE 구독이면 false")
	void freeReturnsFalse() {
		Subscription free = Subscription.createFree(99L, 1L, Instant.now());
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
