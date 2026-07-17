package com.white.handdam.payment.service;

import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.creator.repository.CreatorProfileRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.payment.client.TossConfirmFailureException;
import com.white.handdam.payment.client.TossPaymentsClient;
import com.white.handdam.payment.client.TossTimeoutException;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentPrepareRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentDetailResponse;
import com.white.handdam.payment.dto.response.PaymentPrepareResponse;
import com.white.handdam.payment.dto.response.PaymentSummaryResponse;
import com.white.handdam.payment.dto.toss.TossConfirmRequest;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;
import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.entity.PaymentStatus;
import com.white.handdam.payment.exception.PaymentErrorCode;
import com.white.handdam.payment.repository.PaymentRepository;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long CREATOR_ID = 2L;
    private static final Long PAYMENT_ID = 10L;
    private static final Long SUBSCRIPTION_ID = 20L;
    private static final String ORDER_ID = "HANDDAM-1234567890abcdef";
    private static final String PAYMENT_KEY = "payment-key";
    private static final String IDEMPOTENCY_KEY = "idempotency-key";
    private static final Instant APPROVED_AT = Instant.parse("2026-07-15T01:00:00Z");

    @Mock
    private PaymentTransactionService paymentTransactionService;

    @Mock
    private TossPaymentsClient tossPaymentsClient;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CreatorProfileRepository creatorProfileRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("prepare creates a pending payment using server subscription price")
    void prepareCreatesPendingPaymentUsingServerPrice() {
        Payment payment = pendingPayment(15000);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.USER)));
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID))
                .thenReturn(Optional.of(creatorProfile(15000)));
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(MEMBER_ID, CREATOR_ID))
                .thenReturn(Optional.empty());
        when(paymentTransactionService.createPendingPayment(MEMBER_ID, CREATOR_ID, 15000))
                .thenReturn(payment);

        PaymentPrepareResponse response =
                paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(CREATOR_ID));

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(response.customerKey()).isEqualTo("customer-key");
        assertThat(response.creatorId()).isEqualTo(CREATOR_ID);
        assertThat(response.amount()).isEqualTo(15000);
        assertThat(response.currency()).isEqualTo("KRW");
        assertThat(response.orderName()).contains("creator2");
        verifyNoInteractions(tossPaymentsClient);
    }

    @Test
    @DisplayName("prepare rejects self payment")
    void prepareRejectsSelfPayment() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.CREATOR)));
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.CREATOR)));

        assertThatThrownBy(() -> paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(MEMBER_ID)))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(SubscriptionErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED)
                );
        verifyNoInteractions(tossPaymentsClient);
    }

    @Test
    @DisplayName("prepare rejects missing member")
    void prepareRejectsMissingMember() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(CREATOR_ID)))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("prepare rejects non creator target")
    void prepareRejectsNonCreator() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.USER)));
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.USER)));

        assertThatThrownBy(() -> paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(CREATOR_ID)))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(CreatorErrorCode.CREATOR_NOT_FOUND)
                );
        verifyNoInteractions(creatorProfileRepository);
    }

    @Test
    @DisplayName("prepare rejects missing creator profile")
    void prepareRejectsMissingCreatorProfile() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.USER)));
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(CREATOR_ID)))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("prepare rejects zero paid plan price")
    void prepareRejectsZeroPaidPlanPrice() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.USER)));
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID)).thenReturn(Optional.of(creatorProfile(0)));

        assertThatThrownBy(() -> paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(CREATOR_ID)))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAID_PLAN_NOT_AVAILABLE)
                );
    }

    @Test
    @DisplayName("prepare rejects existing paid subscription")
    void prepareRejectsExistingPaidSubscription() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.USER)));
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID)).thenReturn(Optional.of(creatorProfile(15000)));
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(MEMBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(paidSubscription(SubscriptionStatus.ACTIVE)));

        assertThatThrownBy(() -> paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(CREATOR_ID)))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAID_SUBSCRIPTION_ALREADY_EXISTS)
                );
        verify(paymentTransactionService, never()).createPendingPayment(any(), any(), anyInt());
    }

    @Test
    @DisplayName("prepare allows existing free subscription")
    void prepareAllowsExistingFreeSubscription() {
        Payment payment = pendingPayment(15000);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member(MEMBER_ID, Role.USER)));
        when(memberRepository.findById(CREATOR_ID)).thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR)));
        when(creatorProfileRepository.findByMemberId(CREATOR_ID)).thenReturn(Optional.of(creatorProfile(15000)));
        when(subscriptionRepository.findBySubscriberIdAndCreatorId(MEMBER_ID, CREATOR_ID))
                .thenReturn(Optional.of(Subscription.createFree(MEMBER_ID, CREATOR_ID, APPROVED_AT)));
        when(paymentTransactionService.createPendingPayment(MEMBER_ID, CREATOR_ID, 15000))
                .thenReturn(payment);

        PaymentPrepareResponse response =
                paymentService.prepare(MEMBER_ID, new PaymentPrepareRequest(CREATOR_ID));

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
    }

    @Test
    @DisplayName("getMyPayments reads only the current member payments and batches creator lookup")
    void getMyPaymentsReadsCurrentMemberPayments() {
        Payment payment = payment(MEMBER_ID, CREATOR_ID, PAYMENT_ID, APPROVED_AT);
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findByMemberIdOrderByLatest(MEMBER_ID, pageable))
                .thenReturn(new SliceImpl<>(List.of(payment), pageable, false));
        when(memberRepository.findAllById(any()))
                .thenReturn(List.of(member(CREATOR_ID, Role.CREATOR, "creator", "https://image/creator.png")));

        Slice<PaymentSummaryResponse> responses = paymentService.getMyPayments(MEMBER_ID, pageable);

        assertThat(responses.getContent()).hasSize(1);
        assertThat(responses.getContent().get(0).paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(responses.getContent().get(0).creatorId()).isEqualTo(CREATOR_ID);
        assertThat(responses.getContent().get(0).creatorNickname()).isEqualTo("creator");
        assertThat(responses.getContent().get(0).creatorProfileImageUrl()).isEqualTo("https://image/creator.png");
        verify(paymentRepository).findByMemberIdOrderByLatest(MEMBER_ID, pageable);
        verify(memberRepository).findAllById(any());
    }

    @Test
    @DisplayName("getMyPayments returns an empty slice without creator lookup")
    void getMyPaymentsReturnsEmptySlice() {
        Pageable pageable = PageRequest.of(0, 20);
        when(paymentRepository.findByMemberIdOrderByLatest(MEMBER_ID, pageable))
                .thenReturn(new SliceImpl<>(List.of(), pageable, false));

        Slice<PaymentSummaryResponse> responses = paymentService.getMyPayments(MEMBER_ID, pageable);

        assertThat(responses.getContent()).isEmpty();
        verify(memberRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("getPayment returns detail for the current member payment")
    void getPaymentReturnsCurrentMemberPayment() {
        Payment payment = successfulPaymentForDetail(MEMBER_ID);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(memberRepository.findById(CREATOR_ID))
                .thenReturn(Optional.of(member(CREATOR_ID, Role.CREATOR, "creator", null)));

        PaymentDetailResponse response = paymentService.getPayment(MEMBER_ID, PAYMENT_ID);

        assertThat(response.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(response.subscriptionId()).isEqualTo(SUBSCRIPTION_ID);
        assertThat(response.creatorId()).isEqualTo(CREATOR_ID);
        assertThat(response.creatorNickname()).isEqualTo("creator");
        assertThat(response.amount()).isEqualTo(15000);
        assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.paymentMethod()).isEqualTo("CARD");
        assertThat(response.failureCode()).isNull();
    }

    @Test
    @DisplayName("getPayment rejects a missing payment")
    void getPaymentRejectsMissingPayment() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(MEMBER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAYMENT_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("getPayment rejects another member payment")
    void getPaymentRejectsOwnerMismatch() {
        Payment payment = successfulPaymentForDetail(9L);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.getPayment(MEMBER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.PAYMENT_OWNER_MISMATCH)
                );
        verify(memberRepository, never()).findById(eq(CREATOR_ID));
    }

    @Test
    @DisplayName("confirm calls Toss with DB idempotency key and completes success")
    void confirmCallsTossWithDbIdempotencyKey() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        PaymentConfirmCommand command = command();
        PaymentConfirmResponse response = confirmResponse();
        when(paymentTransactionService.startConfirm(MEMBER_ID, request))
                .thenReturn(PaymentConfirmStartResult.requiresToss(command));
        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class), any()))
                .thenReturn(tossResponse(PAYMENT_KEY, ORDER_ID, 15000, "DONE"));
        when(paymentTransactionService.completeSuccess(command, tossResponse(PAYMENT_KEY, ORDER_ID, 15000, "DONE")))
                .thenReturn(response);

        PaymentConfirmResponse result = paymentService.confirm(MEMBER_ID, request);

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        ArgumentCaptor<TossConfirmRequest> requestCaptor =
                ArgumentCaptor.forClass(TossConfirmRequest.class);
        verify(tossPaymentsClient).confirm(requestCaptor.capture(), org.mockito.Mockito.eq(IDEMPOTENCY_KEY));
        assertThat(requestCaptor.getValue().paymentKey()).isEqualTo(PAYMENT_KEY);
        assertThat(requestCaptor.getValue().orderId()).isEqualTo(ORDER_ID);
        assertThat(requestCaptor.getValue().amount()).isEqualTo(15000);
    }

    @Test
    @DisplayName("confirm does not call Toss when payment lock acquisition fails")
    void confirmDoesNotCallTossWhenPaymentLockAcquisitionFails() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        when(paymentTransactionService.startConfirm(MEMBER_ID, request))
                .thenThrow(new PessimisticLockingFailureException("lock timeout"));

        assertThatThrownBy(() -> paymentService.confirm(MEMBER_ID, request))
                .isInstanceOf(PessimisticLockingFailureException.class);

        verifyNoInteractions(tossPaymentsClient);
        verify(paymentTransactionService, never()).completeSuccess(any(), any());
        verify(paymentTransactionService, never()).failConfirming(any(), any(), any());
    }

    @Test
    @DisplayName("confirm returns existing success response without Toss call")
    void confirmReturnsExistingSuccessWithoutTossCall() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        PaymentConfirmResponse response = confirmResponse();
        when(paymentTransactionService.startConfirm(MEMBER_ID, request))
                .thenReturn(PaymentConfirmStartResult.alreadySucceeded(response));

        PaymentConfirmResponse result = paymentService.confirm(MEMBER_ID, request);

        assertThat(result).isEqualTo(response);
        verifyNoInteractions(tossPaymentsClient);
    }

    @Test
    @DisplayName("confirm marks CONFIRMING payment failed only for definitive Toss rejection")
    void confirmMarksFailedForDefinitiveTossRejection() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        PaymentConfirmCommand command = command();
        when(paymentTransactionService.startConfirm(MEMBER_ID, request))
                .thenReturn(PaymentConfirmStartResult.requiresToss(command));
        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class), any()))
                .thenThrow(new TossConfirmFailureException("REJECT_CARD_COMPANY", "rejected"));

        assertThatThrownBy(() -> paymentService.confirm(MEMBER_ID, request))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.TOSS_CONFIRM_FAILED)
                );
        verify(paymentTransactionService).failConfirming(ORDER_ID, "REJECT_CARD_COMPANY", "rejected");
    }

    @Test
    @DisplayName("confirm keeps CONFIRMING state for Toss timeout")
    void confirmKeepsConfirmingForTossTimeout() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        PaymentConfirmCommand command = command();
        when(paymentTransactionService.startConfirm(MEMBER_ID, request))
                .thenReturn(PaymentConfirmStartResult.requiresToss(command));
        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class), any()))
                .thenThrow(new TossTimeoutException("TIMEOUT", "timeout"));

        assertThatThrownBy(() -> paymentService.confirm(MEMBER_ID, request))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(PaymentErrorCode.TOSS_TIMEOUT)
                );
        verify(paymentTransactionService, never()).failConfirming(any(), any(), any());
    }

    @Test
    @DisplayName("confirm rejects invalid Toss response without completing success")
    void confirmRejectsInvalidTossResponse() {
        PaymentConfirmRequest request = new PaymentConfirmRequest(PAYMENT_KEY, ORDER_ID, 15000);
        PaymentConfirmCommand command = command();
        when(paymentTransactionService.startConfirm(MEMBER_ID, request))
                .thenReturn(PaymentConfirmStartResult.requiresToss(command));
        when(tossPaymentsClient.confirm(any(TossConfirmRequest.class), any()))
                .thenReturn(tossResponse(PAYMENT_KEY, "OTHER-ORDER", 15000, "DONE"));

        assertThatThrownBy(() -> paymentService.confirm(MEMBER_ID, request))
                .isInstanceOfSatisfying(
                        CustomException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(PaymentErrorCode.INVALID_TOSS_RESPONSE)
                );
        verify(paymentTransactionService, never()).completeSuccess(any(), any());
        verify(paymentTransactionService, never()).failConfirming(any(), any(), any());
    }

    private Payment pendingPayment(int amount) {
        Payment payment = Payment.prepare(
                MEMBER_ID,
                CREATOR_ID,
                ORDER_ID,
                IDEMPOTENCY_KEY,
                "customer-key",
                amount
        );
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        return payment;
    }

    private Payment payment(Long memberId, Long creatorId, Long paymentId, Instant createdAt) {
        Payment payment = Payment.prepare(
                memberId,
                creatorId,
                ORDER_ID + "-" + paymentId,
                IDEMPOTENCY_KEY + "-" + paymentId,
                "customer-key-" + paymentId,
                15000
        );
        ReflectionTestUtils.setField(payment, "id", paymentId);
        ReflectionTestUtils.setField(payment, "createdAt", createdAt);
        return payment;
    }

    private Payment successfulPaymentForDetail(Long memberId) {
        Payment payment = payment(memberId, CREATOR_ID, PAYMENT_ID, APPROVED_AT);
        payment.startConfirm();
        payment.completeSuccess(PAYMENT_KEY, "CARD", APPROVED_AT, SUBSCRIPTION_ID);
        return payment;
    }

    private PaymentConfirmCommand command() {
        return new PaymentConfirmCommand(
                PAYMENT_ID,
                MEMBER_ID,
                CREATOR_ID,
                PAYMENT_KEY,
                ORDER_ID,
                15000,
                IDEMPOTENCY_KEY
        );
    }

    private TossConfirmResponse tossResponse(
            String paymentKey,
            String orderId,
            int totalAmount,
            String status
    ) {
        return new TossConfirmResponse(paymentKey, orderId, status, totalAmount, "카드", APPROVED_AT);
    }

    private PaymentConfirmResponse confirmResponse() {
        return new PaymentConfirmResponse(
                PAYMENT_ID,
                ORDER_ID,
                PAYMENT_KEY,
                15000,
                PaymentStatus.SUCCESS,
                "카드",
                APPROVED_AT,
                SUBSCRIPTION_ID,
                SubscriptionLevel.PAID,
                APPROVED_AT,
                APPROVED_AT.atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant()
        );
    }

    private CreatorProfile creatorProfile(int subscriptionPrice) {
        return CreatorProfile.builder()
                .memberId(CREATOR_ID)
                .subscriptionPrice(subscriptionPrice)
                .build();
    }

    private Subscription paidSubscription(SubscriptionStatus status) {
        Subscription subscription = Subscription.createPaid(MEMBER_ID, CREATOR_ID, 15000, APPROVED_AT);
        ReflectionTestUtils.setField(subscription, "status", status);
        return subscription;
    }

    private Member member(Long id, Role role) {
        return member(id, role, "creator" + id, null);
    }

    private Member member(Long id, Role role, String nickname, String profileImageUrl) {
        Member member = Member.createLocalMember("member" + id + "@handdam.com", "password", nickname);
        ReflectionTestUtils.setField(member, "id", id);
        ReflectionTestUtils.setField(member, "role", role);
        ReflectionTestUtils.setField(member, "profileImageUrl", profileImageUrl);
        return member;
    }
}
