package com.white.handdam.payment.repository;

import com.white.handdam.global.persistence.JpaLockHints;
import com.white.handdam.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRepositoryLockHintTest {

    @Test
    @DisplayName("findByOrderIdForUpdate has pessimistic write lock and 3000ms timeout hint")
    void findByOrderIdForUpdateHasLockTimeoutHint() throws NoSuchMethodException {
        Method method = PaymentRepository.class.getMethod("findByOrderIdForUpdate", String.class);

        Lock lock = method.getAnnotation(Lock.class);
        QueryHint hint = lockTimeoutHint(method);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(hint.name()).isEqualTo(JpaLockHints.LOCK_TIMEOUT_HINT);
        assertThat(hint.value()).isEqualTo(JpaLockHints.LOCK_TIMEOUT_MILLIS);
    }

    @Test
    @DisplayName("normal payment queries do not have lock timeout hint")
    void normalQueriesDoNotHaveLockTimeoutHint() throws NoSuchMethodException {
        Method findByOrderId = PaymentRepository.class.getMethod("findByOrderId", String.class);
        Method findByMemberIdAndStatusOrderByLatest = PaymentRepository.class.getMethod(
                "findByMemberIdAndStatusOrderByLatest",
                Long.class,
                PaymentStatus.class,
                Pageable.class
        );

        assertThat(findByOrderId.getAnnotation(Lock.class)).isNull();
        assertThat(findByOrderId.getAnnotation(QueryHints.class)).isNull();
        assertThat(findByMemberIdAndStatusOrderByLatest.getAnnotation(Lock.class)).isNull();
        assertThat(findByMemberIdAndStatusOrderByLatest.getAnnotation(QueryHints.class)).isNull();
    }

    private QueryHint lockTimeoutHint(Method method) {
        QueryHints hints = method.getAnnotation(QueryHints.class);
        assertThat(hints).isNotNull();
        assertThat(hints.value()).hasSize(1);
        return hints.value()[0];
    }
}
