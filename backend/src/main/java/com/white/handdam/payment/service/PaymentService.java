package com.white.handdam.payment.service;

import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.creator.entity.CreatorProfile;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.creator.repository.CreatorProfileRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.payment.client.TossApiException;
import com.white.handdam.payment.client.TossConfirmFailureException;
import com.white.handdam.payment.client.TossPaymentsClient;
import com.white.handdam.payment.client.TossTimeoutException;
import com.white.handdam.payment.dto.request.PaymentConfirmRequest;
import com.white.handdam.payment.dto.request.PaymentFailRequest;
import com.white.handdam.payment.dto.request.PaymentPrepareRequest;
import com.white.handdam.payment.dto.response.PaymentConfirmResponse;
import com.white.handdam.payment.dto.response.PaymentFailResponse;
import com.white.handdam.payment.dto.response.PaymentPrepareResponse;
import com.white.handdam.payment.dto.toss.TossConfirmRequest;
import com.white.handdam.payment.dto.toss.TossConfirmResponse;
import com.white.handdam.payment.entity.Payment;
import com.white.handdam.payment.exception.PaymentErrorCode;
import com.white.handdam.subscription.entity.Subscription;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import com.white.handdam.subscription.exception.SubscriptionErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String TOSS_DONE_STATUS = "DONE";
    private static final String ORDER_NAME_SUFFIX = " 작가 월간 유료 구독";
    private static final int TOSS_ORDER_NAME_MAX_LENGTH = 100;

    private final PaymentTransactionService paymentTransactionService;
    private final TossPaymentsClient tossPaymentsClient;
    private final MemberRepository memberRepository;
    private final CreatorProfileRepository creatorProfileRepository;
    private final SubscriptionRepository subscriptionRepository;

    public PaymentPrepareResponse prepare(Long memberId, PaymentPrepareRequest request) {
        validateRequired(memberId, "memberId");
        validateRequired(request.creatorId(), "creatorId");

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(AuthErrorCode.MEMBER_NOT_FOUND));
        Member creator = validateCreator(request.creatorId());

        if (Objects.equals(member.getId(), creator.getId())) {
            throw new CustomException(SubscriptionErrorCode.SELF_SUBSCRIPTION_NOT_ALLOWED);
        }

        CreatorProfile creatorProfile = creatorProfileRepository.findByMemberId(creator.getId())
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_PROFILE_NOT_FOUND));

        int amount = creatorProfile.getSubscriptionPrice();
        if (amount <= 0) {
            throw new CustomException(PaymentErrorCode.PAID_PLAN_NOT_AVAILABLE);
        }

        validateNoActivePaidSubscription(member.getId(), creator.getId());

        Payment payment = paymentTransactionService.createPendingPayment(member.getId(), creator.getId(), amount);
        String orderName = buildOrderName(creator.getNickname());

        return PaymentPrepareResponse.of(payment, creator.getNickname(), orderName);
    }

    public PaymentConfirmResponse confirm(Long memberId, PaymentConfirmRequest request) {
        validateRequired(memberId, "memberId");

        PaymentConfirmStartResult startResult =
                paymentTransactionService.startConfirm(memberId, request);
        if (startResult.alreadySucceeded()) {
            return startResult.existingResponse();
        }

        PaymentConfirmCommand command = startResult.command();
        try {
            TossConfirmResponse tossResponse = tossPaymentsClient.confirm(
                    new TossConfirmRequest(
                            command.paymentKey(),
                            command.orderId(),
                            command.amount()
                    ),
                    command.idempotencyKey()
            );
            validateTossResponse(command, tossResponse);
            return paymentTransactionService.completeSuccess(command, tossResponse);
        } catch (TossConfirmFailureException exception) {
            paymentTransactionService.failConfirming(
                    command.orderId(),
                    exception.getFailureCode(),
                    exception.getSafeMessage()
            );
            throw new CustomException(PaymentErrorCode.TOSS_CONFIRM_FAILED);
        } catch (TossTimeoutException exception) {
            throw new CustomException(PaymentErrorCode.TOSS_TIMEOUT);
        } catch (TossApiException exception) {
            throw new CustomException(PaymentErrorCode.TOSS_API_ERROR);
        }
    }

    public PaymentFailResponse fail(Long memberId, PaymentFailRequest request) {
        validateRequired(memberId, "memberId");
        return paymentTransactionService.fail(memberId, request);
    }

    private Member validateCreator(Long creatorId) {
        Member creator = memberRepository.findById(creatorId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));

        if (creator.getRole() != Role.CREATOR) {
            throw new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND);
        }

        return creator;
    }

    private void validateNoActivePaidSubscription(Long memberId, Long creatorId) {
        subscriptionRepository.findBySubscriberIdAndCreatorId(memberId, creatorId)
                .filter(subscription -> subscription.isPaid())
                .ifPresent(subscription -> {
                    throw new CustomException(PaymentErrorCode.PAID_SUBSCRIPTION_ALREADY_EXISTS);
                });
    }

    private void validateTossResponse(
            PaymentConfirmCommand command,
            TossConfirmResponse tossResponse
    ) {
        if (tossResponse == null
                || !Objects.equals(command.paymentKey(), tossResponse.paymentKey())
                || !Objects.equals(command.orderId(), tossResponse.orderId())
                || command.amount() != tossResponse.totalAmount()
                || !TOSS_DONE_STATUS.equals(tossResponse.status())
                || tossResponse.approvedAt() == null) {
            throw new CustomException(PaymentErrorCode.INVALID_TOSS_RESPONSE);
        }
    }

    private String buildOrderName(String creatorNickname) {
        String orderName = creatorNickname + ORDER_NAME_SUFFIX;
        if (orderName.length() <= TOSS_ORDER_NAME_MAX_LENGTH) {
            return orderName;
        }
        return orderName.substring(0, TOSS_ORDER_NAME_MAX_LENGTH);
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
