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

    private static final Instant TARGET_START = Instant.parse("2026-08-13T00:00:00Z");
    private static final Instant TARGET_END = Instant.parse("2026-08-14T00:00:00Z");

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
                eq(TARGET_START),
                eq(TARGET_END),
                any(Pageable.class)
        )).thenReturn(List.of(target(1L, TARGET_START)));

        List<SubscriptionExpiringSoonTarget> targets =
                expiringSoonService.findExpiringSoonTargets(TARGET_START, TARGET_END, 100, 2);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(subscriptionRepository).findExpiringSoonSubscriptionTargets(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(TARGET_START),
                eq(TARGET_END),
                pageableCaptor.capture()
        );
        assertThat(targets).hasSize(1);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("findExpiringSoonTargets rejects invalid batch size")
    void findExpiringSoonTargetsRejectsInvalidBatchSize() {
        assertThatThrownBy(() -> expiringSoonService.findExpiringSoonTargets(TARGET_START, TARGET_END, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchSize must be positive");
    }

    @Test
    @DisplayName("publishExpiringSoonEvents processes every page and counts results")
    void publishExpiringSoonEventsProcessesEveryPageAndCountsResults() {
        SubscriptionExpiringSoonTarget first = target(1L, TARGET_START.plusSeconds(10));
        SubscriptionExpiringSoonTarget second = target(2L, TARGET_START.plusSeconds(20));
        SubscriptionExpiringSoonTarget third = target(3L, TARGET_START.plusSeconds(30));
        when(subscriptionRepository.findExpiringSoonSubscriptionTargets(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(TARGET_START),
                eq(TARGET_END),
                any(Pageable.class)
        )).thenReturn(List.of(first, second))
                .thenReturn(List.of(third));
        when(transactionService.publishExpiringSoonEvent(1L, first.currentPeriodEndAt(), TARGET_START, TARGET_END))
                .thenReturn(true);
        when(transactionService.publishExpiringSoonEvent(2L, second.currentPeriodEndAt(), TARGET_START, TARGET_END))
                .thenReturn(false);
        when(transactionService.publishExpiringSoonEvent(3L, third.currentPeriodEndAt(), TARGET_START, TARGET_END))
                .thenThrow(new IllegalStateException("broken subscription"));

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(TARGET_START, TARGET_END, 2);

        assertThat(result.candidateCount()).isEqualTo(3);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        verify(transactionService).publishExpiringSoonEvent(1L, first.currentPeriodEndAt(), TARGET_START, TARGET_END);
        verify(transactionService).publishExpiringSoonEvent(2L, second.currentPeriodEndAt(), TARGET_START, TARGET_END);
        verify(transactionService).publishExpiringSoonEvent(3L, third.currentPeriodEndAt(), TARGET_START, TARGET_END);
    }

    @Test
    @DisplayName("publishExpiringSoonEvents continues after lock acquisition failure")
    void publishExpiringSoonEventsContinuesAfterLockFailure() {
        SubscriptionExpiringSoonTarget first = target(1L, TARGET_START.plusSeconds(10));
        SubscriptionExpiringSoonTarget second = target(2L, TARGET_START.plusSeconds(20));
        when(subscriptionRepository.findExpiringSoonSubscriptionTargets(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(TARGET_START),
                eq(TARGET_END),
                any(Pageable.class)
        )).thenReturn(List.of(first, second));
        when(transactionService.publishExpiringSoonEvent(1L, first.currentPeriodEndAt(), TARGET_START, TARGET_END))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));
        when(transactionService.publishExpiringSoonEvent(2L, second.currentPeriodEndAt(), TARGET_START, TARGET_END))
                .thenReturn(true);

        SubscriptionExpiringSoonResult result =
                expiringSoonService.publishExpiringSoonEvents(TARGET_START, TARGET_END, 100);

        assertThat(result.candidateCount()).isEqualTo(2);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
    }

    private SubscriptionExpiringSoonTarget target(Long subscriptionId, Instant currentPeriodEndAt) {
        return new SubscriptionExpiringSoonTarget(subscriptionId, currentPeriodEndAt);
    }
}
