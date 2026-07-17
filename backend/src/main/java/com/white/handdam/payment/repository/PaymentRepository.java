package com.white.handdam.payment.repository;

import com.white.handdam.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    @Query("""
            select payment
            from Payment payment
            where payment.memberId = :memberId
            order by payment.createdAt desc, payment.id desc
            """)
    Slice<Payment> findByMemberIdOrderByLatest(
            @Param("memberId") Long memberId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    boolean existsByPgTransactionIdAndIdNot(String pgTransactionId, Long id);
}
