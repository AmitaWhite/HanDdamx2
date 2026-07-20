package com.white.handdam.payment.event;

/**
 * 결제가 최종 승인되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code PaymentNotificationListener} 가 구독해
 * 결제한 본인에게 {@code PAYMENT_SUCCESS} 알림을 만든다.
 *
 * <h3>발행 위치 — PaymentService 가 아니다</h3>
 * {@code payment/service/PaymentTransactionService#completeSuccess} — {@code @Transactional} 안,
 * {@code return} 직전 (현재 117번 줄 부근).
 *
 * <p><b>주의:</b> {@code PaymentService#confirm} 에는 {@code @Transactional} 이 없다.
 * 거기서 발행하면 트랜잭션이 없어 {@code @TransactionalEventListener(AFTER_COMMIT)} 가
 * <b>아예 실행되지 않는다.</b> 쓰기 트랜잭션은 전부 {@code PaymentTransactionService} 에 있다.
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * eventPublisher.publishEvent(new PaymentSucceededEvent(
 *     payment.getId(),
 *     payment.getMemberId(),   // 결제자 = 알림 수신자
 *     payment.getAmount(),
 *     command.orderId()
 * ));
 * return PaymentConfirmResponse.of(payment, subscription);
 * }</pre>
 *
 * <h3>시스템 알림 — senderId 는 null</h3>
 * 이 알림에는 "행위자"가 없다. 리스너는 {@code senderId} 로 {@code null} 을 넘긴다.
 * 결제자를 {@code senderId} 에도 넣으면 자기알림 억제 로직에 걸려
 * <b>알림이 예외도 로그도 없이 사라진다.</b>
 *
 * @param paymentId 결제 ID (알림 클릭 시 이동 대상)
 * @param memberId  결제자 = 알림 수신자
 * @param amount    결제 금액 (알림 문구용)
 * @param orderId   주문 번호 (로그 추적용)
 */
public record PaymentSucceededEvent(
	Long paymentId,
	Long memberId,
	int amount,
	String orderId
) {
}
