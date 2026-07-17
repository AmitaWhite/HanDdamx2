package com.white.handdam.subscription.repository;

import com.white.handdam.global.persistence.JpaLockHints;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.lang.reflect.Method;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionRepositoryLockHintTest {

    @Test
    @DisplayName("findByIdForUpdate has pessimistic write lock and 3000ms timeout hint")
    void findByIdForUpdateHasLockTimeoutHint() throws NoSuchMethodException {
        Method method = SubscriptionRepository.class.getMethod("findByIdForUpdate", Long.class);

        assertLockTimeoutHint(method);
    }

    @Test
    @DisplayName("findBySubscriberIdAndCreatorIdForUpdate has pessimistic write lock and 3000ms timeout hint")
    void findBySubscriberIdAndCreatorIdForUpdateHasLockTimeoutHint() throws NoSuchMethodException {
        Method method = SubscriptionRepository.class.getMethod(
                "findBySubscriberIdAndCreatorIdForUpdate",
                Long.class,
                Long.class
        );

        assertLockTimeoutHint(method);
    }

    @Test
    @DisplayName("normal subscription queries do not have lock timeout hint")
    void normalQueriesDoNotHaveLockTimeoutHint() throws NoSuchMethodException {
        Method findBySubscriberIdAndCreatorId = SubscriptionRepository.class.getMethod(
                "findBySubscriberIdAndCreatorId",
                Long.class,
                Long.class
        );
        Method findExpiredSubscriptionIds = SubscriptionRepository.class.getMethod(
                "findExpiredSubscriptionIds",
                SubscriptionLevel.class,
                SubscriptionStatus.class,
                Instant.class,
                Pageable.class
        );
        Method findBySubscriberIdOrderByStartedAtDesc = SubscriptionRepository.class.getMethod(
                "findBySubscriberIdOrderByStartedAtDesc",
                Long.class
        );

        assertNoLockTimeoutHint(findBySubscriberIdAndCreatorId);
        assertNoLockTimeoutHint(findExpiredSubscriptionIds);
        assertNoLockTimeoutHint(findBySubscriberIdOrderByStartedAtDesc);
    }

    private void assertLockTimeoutHint(Method method) {
        Lock lock = method.getAnnotation(Lock.class);
        QueryHints hints = method.getAnnotation(QueryHints.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(hints).isNotNull();
        assertThat(hints.value()).hasSize(1);

        QueryHint hint = hints.value()[0];
        assertThat(hint.name()).isEqualTo(JpaLockHints.LOCK_TIMEOUT_HINT);
        assertThat(hint.value()).isEqualTo(JpaLockHints.LOCK_TIMEOUT_MILLIS);
    }

    private void assertNoLockTimeoutHint(Method method) {
        assertThat(method.getAnnotation(Lock.class)).isNull();
        assertThat(method.getAnnotation(QueryHints.class)).isNull();
    }
}
