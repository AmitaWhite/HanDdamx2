package com.white.handdam.creator.event;

/**
 * 크리에이터 전환 신청이 승인되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code CreatorNotificationListener} 가 구독해
 * 신청자에게 {@code CREATOR_APPLICATION_APPROVED} 알림을 만든다.
 *
 * <h3>발행 위치 — save() 호출이 없다</h3>
 * {@code creator/service/CreatorApplicationService#approve} — {@code @Transactional} 안,
 * {@code application.approve(adminId)} 직후 {@code return} 직전 (현재 158번 줄 부근).
 *
 * <p><b>주의:</b> 이 메서드는 {@code save()} 를 호출하지 않는다.
 * {@code member.changeRoleToCreator()} 와 {@code application.approve(adminId)} 모두
 * 영속 상태 엔티티를 더티 체킹으로 변경한다.
 * 규칙은 "save 직후"가 아니라 <b>"상태 변경 직후, {@code @Transactional} 안"</b> 이다.
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * application.approve(adminId);
 * eventPublisher.publishEvent(new CreatorApplicationApprovedEvent(
 *     applicationId,                // 파라미터
 *     application.getMemberId()     // 신청자 = 알림 수신자
 * ));
 * }</pre>
 *
 * <h3>시스템 알림 — senderId 는 null</h3>
 * 승인한 관리자({@code adminId})를 {@code senderId} 로 넘기지 않는다.
 * 신청자에게 관리자 ID 를 노출할 이유가 없고, 심사 결과는 시스템 통지에 가깝다.
 * 리스너가 {@code senderId = null} 로 만든다.
 *
 * @param applicationId 신청 ID (알림 클릭 시 이동 대상)
 * @param applicantId   신청자 = 알림 수신자 ({@code application.getMemberId()})
 */
public record CreatorApplicationApprovedEvent(
	Long applicationId,
	Long applicantId
) {
}
