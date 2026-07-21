package com.white.handdam.subscription.repository;

import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=test_secret_for_subscription_repository_1234567890"
})
@ActiveProfiles("test")
@Transactional
class SubscriptionRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final Instant TARGET_START = Instant.parse("2026-08-12T15:00:00Z");
    private static final Instant TARGET_END = Instant.parse("2026-08-13T15:00:00Z");

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("findExpiredSubscriptionIds returns only expired cancel-scheduled paid subscriptions")
    void findExpiredSubscriptionIdsReturnsOnlyTargets() {
        Subscription oldestTarget = saveScheduledPaid(1L, 101L, NOW.minusSeconds(20));
        Subscription nextTarget = saveScheduledPaid(2L, 102L, NOW.minusSeconds(10));
        saveActivePaid(3L, 103L, NOW.minusSeconds(30));
        saveFree(4L, 104L);
        saveScheduledPaid(5L, 105L, NOW.plusSeconds(1));
        saveScheduledPaidWithoutPeriodEnd(6L, 106L);

        List<Long> subscriptionIds = subscriptionRepository.findExpiredSubscriptionIds(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                NOW,
                PageRequest.of(0, 10)
        );

        assertThat(subscriptionIds).containsExactly(oldestTarget.getId(), nextTarget.getId());
    }

    @Test
    @DisplayName("findExpiredSubscriptionIds orders by period end and id")
    void findExpiredSubscriptionIdsOrdersByPeriodEndAndId() {
        Subscription secondByPeriod = saveScheduledPaid(1L, 101L, NOW.minusSeconds(10));
        Subscription firstByPeriodAndId = saveScheduledPaid(2L, 102L, NOW.minusSeconds(20));
        Subscription secondByPeriodAndId = saveScheduledPaid(3L, 103L, NOW.minusSeconds(20));

        List<Long> subscriptionIds = subscriptionRepository.findExpiredSubscriptionIds(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                NOW,
                PageRequest.of(0, 10)
        );

        assertThat(subscriptionIds).containsExactly(
                firstByPeriodAndId.getId(),
                secondByPeriodAndId.getId(),
                secondByPeriod.getId()
        );
    }

    @Test
    @DisplayName("findExpiredSubscriptionIds applies batch size")
    void findExpiredSubscriptionIdsAppliesBatchSize() {
        Subscription firstTarget = saveScheduledPaid(1L, 101L, NOW.minusSeconds(20));
        saveScheduledPaid(2L, 102L, NOW.minusSeconds(10));

        List<Long> subscriptionIds = subscriptionRepository.findExpiredSubscriptionIds(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                NOW,
                PageRequest.of(0, 1)
        );

        assertThat(subscriptionIds).containsExactly(firstTarget.getId());
    }

    @Test
    @DisplayName("findExpiringSoonSubscriptionTargets returns only cancel-scheduled paid subscriptions in target date range")
    void findExpiringSoonSubscriptionTargetsReturnsOnlyTargets() {
        Subscription startTarget = saveScheduledPaid(1L, 101L, TARGET_START);
        Subscription insideTarget = saveScheduledPaid(2L, 102L, TARGET_START.plusSeconds(60));
        saveScheduledPaid(3L, 103L, TARGET_START.minusSeconds(1));
        saveScheduledPaid(4L, 104L, TARGET_END);
        saveScheduledPaid(5L, 105L, TARGET_END.plusSeconds(1));
        saveActivePaid(6L, 106L, TARGET_START.plusSeconds(60));
        saveFree(7L, 107L);
        saveScheduledPaidWithoutPeriodEnd(8L, 108L);

        List<SubscriptionExpiringSoonTarget> targets = subscriptionRepository.findExpiringSoonSubscriptionTargets(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                TARGET_START,
                TARGET_END,
                PageRequest.of(0, 10)
        );

        assertThat(targets)
                .extracting(target -> target.subscriptionId())
                .containsExactly(startTarget.getId(), insideTarget.getId());
    }

    @Test
    @DisplayName("findExpiringSoonSubscriptionTargets orders by period end and id")
    void findExpiringSoonSubscriptionTargetsOrdersByPeriodEndAndId() {
        Subscription secondByPeriod = saveScheduledPaid(1L, 101L, TARGET_START.plusSeconds(20));
        Subscription firstByPeriodAndId = saveScheduledPaid(2L, 102L, TARGET_START.plusSeconds(10));
        Subscription secondByPeriodAndId = saveScheduledPaid(3L, 103L, TARGET_START.plusSeconds(10));

        List<SubscriptionExpiringSoonTarget> targets = subscriptionRepository.findExpiringSoonSubscriptionTargets(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                TARGET_START,
                TARGET_END,
                PageRequest.of(0, 10)
        );

        assertThat(targets)
                .extracting(target -> target.subscriptionId())
                .containsExactly(
                        firstByPeriodAndId.getId(),
                        secondByPeriodAndId.getId(),
                        secondByPeriod.getId()
                );
    }

    @Test
    @DisplayName("findExpiringSoonSubscriptionTargets supports paging")
    void findExpiringSoonSubscriptionTargetsSupportsPaging() {
        Subscription firstTarget = saveScheduledPaid(1L, 101L, TARGET_START.plusSeconds(10));
        Subscription secondTarget = saveScheduledPaid(2L, 102L, TARGET_START.plusSeconds(20));
        Subscription thirdTarget = saveScheduledPaid(3L, 103L, TARGET_START.plusSeconds(30));

        List<SubscriptionExpiringSoonTarget> firstPage = subscriptionRepository.findExpiringSoonSubscriptionTargets(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                TARGET_START,
                TARGET_END,
                PageRequest.of(0, 2)
        );
        List<SubscriptionExpiringSoonTarget> secondPage = subscriptionRepository.findExpiringSoonSubscriptionTargets(
                SubscriptionLevel.PAID,
                SubscriptionStatus.CANCEL_SCHEDULED,
                TARGET_START,
                TARGET_END,
                PageRequest.of(1, 2)
        );

        assertThat(firstPage)
                .extracting(target -> target.subscriptionId())
                .containsExactly(firstTarget.getId(), secondTarget.getId());
        assertThat(secondPage)
                .extracting(target -> target.subscriptionId())
                .containsExactly(thirdTarget.getId());
    }

    private Subscription saveScheduledPaid(Long subscriberId, Long creatorId, Instant periodEndAt) {
        Subscription subscription = Subscription.createPaid(
                subscriberId,
                creatorId,
                15000,
                NOW.minusSeconds(60)
        );
        subscription.scheduleCancellation(NOW.minusSeconds(30));
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", periodEndAt);
        return subscriptionRepository.save(subscription);
    }

    private Subscription saveScheduledPaidWithoutPeriodEnd(Long subscriberId, Long creatorId) {
        Subscription subscription = saveScheduledPaid(subscriberId, creatorId, NOW.minusSeconds(20));
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", null);
        return subscription;
    }

    private Subscription saveActivePaid(Long subscriberId, Long creatorId, Instant periodEndAt) {
        Subscription subscription = Subscription.createPaid(
                subscriberId,
                creatorId,
                15000,
                NOW.minusSeconds(60)
        );
        ReflectionTestUtils.setField(subscription, "currentPeriodEndAt", periodEndAt);
        return subscriptionRepository.save(subscription);
    }

    private Subscription saveFree(Long subscriberId, Long creatorId) {
        Subscription subscription = Subscription.createFree(subscriberId, creatorId, NOW.minusSeconds(60));
        return subscriptionRepository.save(subscription);
    }
}
