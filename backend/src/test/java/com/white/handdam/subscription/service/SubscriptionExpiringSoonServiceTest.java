package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionExpiringSoonTarget;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpiringSoonServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");
    private static final Instant THRESHOLD = Instant.parse("2026-08-13T00:00:00Z");

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionExpiringSoonTransactionService transactionService;

    @InjectMocks
    private SubscriptionExpiringSoonService expiringSoonService;

    @Test
    @DisplayName("findExpiringSoonTargets queries cancel scheduled paid subscriptions with page size")
    void findExpiringSoonTargetsQueriesTargetsWithPageSize() {
        when(subscriptionRepository.findExpiringSoonSubscriptionTargets(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                eq(THRESHOLD),
                any(Pageable.class)
        )).thenReturn(List.of(target(1L, THRESHOLD)));

        List<SubscriptionExpiringSoonTarget> targets =
                expiringSoonService.findExpiringSoonTargets(NOW, THRESHOLD, 100, 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(subscriptionRepository).findExpiringSoonSubscriptionTargets(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                eq(THRESHOLD),
                pageableCaptor.capture()
        );
        assertThat(targets).hasSize(1);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("findExpiringSoonTargets rejects invalid batch size")
    void findExpiringSoonTargetsRejectsInvalidBatchSize() {
        assertThatThrownBy(() -> expiringSoonService.findExpiringSoonTargets(NOW, THRESHOLD, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchSize must be positive");
    }

    @Test
    @DisplayName("publishExpiringSoonEvents processes every page and counts results")
    void publishExpiringSoonEventsProcessesEveryPageAndCountsResults() {
        SubscriptionExpiringSoonTarget first = target(1L, NOW.plusSeconds(10));
        SubscriptionExpiringSoonTarget second = target(2L, NOW.plusSeconds(20));
        SubscriptionExpiringSoonTarget third = target(3L, NOW.plusSeconds(30));
        when(subscriptionRepository.findExpiringSoonSubscriptionTargets(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                eq(THRESHOLD),
                any(Pageable.class)
        )).thenReturn(List.of(first, second))
                .thenReturn(List.of(third));
        when(transactionService.publishExpiringSoonEvent(1L, first.currentPeriodEndAt(), NOW, THRESHOLD))
                .thenReturn(true);
        when(transactionService.publishExpiringSoonEvent(2L, second.currentPeriodEndAt(), NOW, THRESHOLD))
                .thenReturn(false);
        when(transactionService.publishExpiringSoonEvent(3L, third.currentPeriodEndAt(), NOW, THRESHOLD))
                .thenThrow(new IllegalStateException("broken subscription"));

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(NOW, THRESHOLD, 2);

        assertThat(result.candidateCount()).isEqualTo(3);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        verify(transactionService).publishExpiringSoonEvent(1L, first.currentPeriodEndAt(), NOW, THRESHOLD);
        verify(transactionService).publishExpiringSoonEvent(2L, second.currentPeriodEndAt(), NOW, THRESHOLD);
        verify(transactionService).publishExpiringSoonEvent(3L, third.currentPeriodEndAt(), NOW, THRESHOLD);
    }

    @Test
    @DisplayName("publishExpiringSoonEvents continues after lock acquisition failure")
    void publishExpiringSoonEventsContinuesAfterLockFailure() {
        SubscriptionExpiringSoonTarget first = target(1L, NOW.plusSeconds(10));
        SubscriptionExpiringSoonTarget second = target(2L, NOW.plusSeconds(20));
        when(subscriptionRepository.findExpiringSoonSubscriptionTargets(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                eq(THRESHOLD),
                any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(transactionService.publishExpiringSoonEvent(1L, first.currentPeriodEndAt(), NOW, THRESHOLD))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));
        when(transactionService.publishExpiringSoonEvent(2L, second.currentPeriodEndAt(), NOW, THRESHOLD))
                .thenReturn(true);

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(NOW, THRESHOLD, 100);

        assertThat(result.candidateCount()).isEqualTo(2);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
    }

    private SubscriptionExpiringSoonTarget target(Long subscriptionId, Instant currentPeriodEndAt) {
        return new SubscriptionExpiringSoonTarget(subscriptionId, currentPeriodEndAt);
    }
}
