package com.white.handdam.payment.repository;

import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=test_secret_for_payment_repository_1234567890"
})
@ActiveProfiles("test")
@Transactional
class PaymentRepositoryTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 9L;
    private static final Long CREATOR_ID = 2L;
    private static final int AMOUNT = 15000;
    private static final Instant BASE_TIME = Instant.parse("2026-07-22T01:00:00Z");

    @Autowired
    private PaymentRepository paymentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @Test
    @DisplayName("findByMemberIdAndStatusOrderByLatest returns only current member SUCCESS payments")
    void findByMemberIdAndStatusOrderByLatestReturnsOnlyCurrentMemberSuccessPayments() {
        Payment olderSuccess = savePayment(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                BASE_TIME.plusSeconds(10),
                "current-success-old",
                null
        );
        Payment newerSuccess = savePayment(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                BASE_TIME.plusSeconds(20),
                "current-success-new",
                "CARD"
        );
        savePayment(MEMBER_ID, PaymentStatus.PENDING, BASE_TIME.plusSeconds(50), "current-pending");
        savePayment(MEMBER_ID, PaymentStatus.CONFIRMING, BASE_TIME.plusSeconds(40), "current-confirming");
        savePayment(MEMBER_ID, PaymentStatus.FAILED, BASE_TIME.plusSeconds(30), "current-failed");
        savePayment(MEMBER_ID, PaymentStatus.CANCELED, BASE_TIME.plusSeconds(60), "current-canceled");
        savePayment(OTHER_MEMBER_ID, PaymentStatus.SUCCESS, BASE_TIME.plusSeconds(70), "other-success");
        entityManager.clear();

        Slice<Payment> result = paymentRepository.findByMemberIdAndStatusOrderByLatest(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Payment::getId)
                .containsExactly(newerSuccess.getId(), olderSuccess.getId());
        assertThat(result.getContent())
                .extracting(Payment::getMemberId)
                .containsOnly(MEMBER_ID);
        assertThat(result.getContent())
                .extracting(Payment::getStatus)
                .containsOnly(PaymentStatus.SUCCESS);
        assertThat(result.getContent())
                .extracting(Payment::getPaymentMethod)
                .containsExactly("CARD", null);
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("findByMemberIdAndStatusOrderByLatest slices only SUCCESS payments in latest order")
    void findByMemberIdAndStatusOrderByLatestSlicesOnlySuccessPayments() {
        Payment oldestSuccess = savePayment(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                BASE_TIME.plusSeconds(10),
                "slice-success-oldest"
        );
        Payment middleSuccess = savePayment(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                BASE_TIME.plusSeconds(20),
                "slice-success-middle"
        );
        Payment latestSuccess = savePayment(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                BASE_TIME.plusSeconds(30),
                "slice-success-latest"
        );
        savePayment(MEMBER_ID, PaymentStatus.PENDING, BASE_TIME.plusSeconds(50), "slice-pending");
        savePayment(MEMBER_ID, PaymentStatus.FAILED, BASE_TIME.plusSeconds(40), "slice-failed");
        entityManager.clear();

        Slice<Payment> firstPage = paymentRepository.findByMemberIdAndStatusOrderByLatest(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                PageRequest.of(0, 2)
        );
        Slice<Payment> secondPage = paymentRepository.findByMemberIdAndStatusOrderByLatest(
                MEMBER_ID,
                PaymentStatus.SUCCESS,
                PageRequest.of(1, 2)
        );

        assertThat(firstPage.getContent())
                .extracting(Payment::getId)
                .containsExactly(latestSuccess.getId(), middleSuccess.getId());
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.getContent())
                .extracting(Payment::getId)
                .containsExactly(oldestSuccess.getId());
        assertThat(secondPage.hasNext()).isFalse();
    }

    private Payment savePayment(
            Long memberId,
            PaymentStatus status,
            Instant createdAt,
            String suffix
    ) {
        return savePayment(memberId, status, createdAt, suffix, "CARD");
    }

    private Payment savePayment(
            Long memberId,
            PaymentStatus status,
            Instant createdAt,
            String suffix,
            String paymentMethod
    ) {
        Payment payment = Payment.prepare(
                memberId,
                CREATOR_ID,
                "ORDER-" + suffix,
                "IDEMPOTENCY-" + suffix,
                "CUSTOMER-" + suffix,
                AMOUNT
        );

        switch (status) {
            case PENDING -> {
            }
            case CONFIRMING -> payment.startConfirm();
            case SUCCESS -> {
                payment.startConfirm();
                payment.completeSuccess("PAYMENT-" + suffix, paymentMethod, createdAt, 1000L);
            }
            case FAILED -> payment.failPending("FAIL-" + suffix, "failed");
            case CANCELED -> ReflectionTestUtils.setField(payment, "status", PaymentStatus.CANCELED);
        }

        Payment saved = paymentRepository.saveAndFlush(payment);
        setCreatedAt(saved.getId(), createdAt);
        return saved;
    }

    private void setCreatedAt(Long paymentId, Instant createdAt) {
        entityManager.createNativeQuery("update payment set created_at = ? where id = ?")
                .setParameter(1, Timestamp.from(createdAt))
                .setParameter(2, paymentId)
                .executeUpdate();
        entityManager.flush();
    }
}
