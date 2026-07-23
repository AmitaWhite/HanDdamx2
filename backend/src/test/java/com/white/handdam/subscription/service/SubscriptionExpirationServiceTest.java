package com.white.handdam.subscription.service;

import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.PessimisticLockingFailureException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionExpirationTransactionService transactionService;

    @InjectMocks
    private SubscriptionExpirationService expirationService;

    @Test
    @DisplayName("findExpiredSubscriptionIds queries cancel scheduled paid subscriptions with batch size")
    void findExpiredSubscriptionIdsQueriesTargetWithBatchSize() {
        when(subscriptionRepository.findExpiredSubscriptionIds(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(1L, 2L));

        List<Long> subscriptionIds = expirationService.findExpiredSubscriptionIds(NOW, 100);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(subscriptionRepository).findExpiredSubscriptionIds(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                pageableCaptor.capture()
        );
        assertThat(subscriptionIds).containsExactly(1L, 2L);
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("findExpiredSubscriptionIds rejects invalid batch size")
    void findExpiredSubscriptionIdsRejectsInvalidBatchSize() {
        assertThatThrownBy(() -> expirationService.findExpiredSubscriptionIds(NOW, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("batchSize must be positive");
    }

    @Test
    @DisplayName("expireScheduledSubscriptions counts converted skipped and failed items")
    void expireScheduledSubscriptionsCountsEachResult() {
        when(subscriptionRepository.findExpiredSubscriptionIds(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(1L, 2L, 3L));
        when(transactionService.convertExpiredSubscriptionToFree(1L, NOW)).thenReturn(true);
        when(transactionService.convertExpiredSubscriptionToFree(2L, NOW)).thenReturn(false);
        when(transactionService.convertExpiredSubscriptionToFree(3L, NOW))
                .thenThrow(new IllegalStateException("broken subscription"));

        SubscriptionExpirationResult result = expirationService.expireScheduledSubscriptions(NOW, 100);

        assertThat(result.candidateCount()).isEqualTo(3);
        assertThat(result.convertedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
        verify(transactionService).convertExpiredSubscriptionToFree(1L, NOW);
        verify(transactionService).convertExpiredSubscriptionToFree(2L, NOW);
        verify(transactionService).convertExpiredSubscriptionToFree(3L, NOW);
    }

    @Test
    @DisplayName("expireScheduledSubscriptions continues after one lock acquisition failure")
    void expireScheduledSubscriptionsContinuesAfterLockAcquisitionFailure() {
        when(subscriptionRepository.findExpiredSubscriptionIds(
                eq(SubscriptionLevel.PAID),
                eq(SubscriptionStatus.CANCEL_SCHEDULED),
                eq(NOW),
                any(Pageable.class)
        )).thenReturn(List.of(1L, 2L));
        when(transactionService.convertExpiredSubscriptionToFree(1L, NOW))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));
        when(transactionService.convertExpiredSubscriptionToFree(2L, NOW)).thenReturn(true);

        SubscriptionExpirationResult result = expirationService.expireScheduledSubscriptions(NOW, 100);

        assertThat(result.candidateCount()).isEqualTo(2);
        assertThat(result.convertedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        verify(transactionService).convertExpiredSubscriptionToFree(1L, NOW);
        verify(transactionService).convertExpiredSubscriptionToFree(2L, NOW);
    }
}
