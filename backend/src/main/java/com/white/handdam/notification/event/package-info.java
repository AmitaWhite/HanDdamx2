/**
 * 도메인 이벤트를 구독해 알림으로 변환하는 리스너들.
 *
 * <h2>구조</h2>
 * <pre>
 * [도메인 서비스]  @Transactional 안에서 상태 변경 후 publishEvent(...)
 *       │
 *       │  ── 커밋 ──
 *       ▼
 * [XxxNotificationListener]  이벤트 → "누구에게 갈 알림인가" 로 번역
 *       ▼
 * [NotificationService.create]  저장 + (커밋 후) 실시간 전송
 * </pre>
 *
 * <p>의존 방향은 <b>{@code notification} → 각 도메인</b> 한쪽으로만 흐른다.
 * 도메인은 {@code notification} 패키지를 import 하지 않고, 자기 이벤트만 발행한다.
 *
 * <h2>모든 핸들러의 3중 안전장치</h2>
 * <ul>
 *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)} — 원본 트랜잭션이 롤백되면
 *       실행되지 않아 "없는 데이터에 대한 알림"을 막는다</li>
 *   <li>{@code @Async("notificationExecutor")} — 별도 스레드로 빠져 알림 처리가
 *       원본 API 응답을 지연시키지 않는다</li>
 *   <li>{@code try/catch} — 알림 실패가 원본 도메인 동작을 깨뜨리지 않는다.
 *       {@code afterCommit} 에서 나간 예외는 커밋 호출자까지 전파되므로 반드시 필요하다</li>
 * </ul>
 *
 * <h2>이벤트 → 알림 매핑</h2>
 * <table border="1">
 *   <caption>현재 연결된 이벤트</caption>
 *   <tr><th>이벤트</th><th>알림 타입</th><th>수신자</th><th>이동 대상</th></tr>
 *   <tr><td>{@code ChatMessageSentEvent}</td><td>CHAT_MESSAGE</td>
 *       <td>대화 상대</td><td>CHAT_ROOM</td></tr>
 *   <tr><td>{@code FeedCommentCreatedEvent}</td><td>FEED_COMMENT</td>
 *       <td>피드 작성자</td><td>FEED</td></tr>
 *   <tr><td>{@code FeedReplyCreatedEvent}</td><td>FEED_REPLY</td>
 *       <td><b>부모 댓글 작성자</b></td><td>FEED</td></tr>
 *   <tr><td>{@code BoardCommentCreatedEvent}</td><td>BOARD_COMMENT</td>
 *       <td>게시글 작성자</td><td>BOARD_POST</td></tr>
 *   <tr><td>{@code BoardReplyCreatedEvent}</td><td>BOARD_REPLY</td>
 *       <td><b>부모 댓글 작성자</b></td><td>BOARD_POST</td></tr>
 *   <tr><td>{@code BoardAnswerCreatedEvent}</td><td>BOARD_ANSWER</td>
 *       <td>질문한 사람</td><td>BOARD_POST</td></tr>
 *   <tr><td>{@code PaymentSucceededEvent}</td><td>PAYMENT_SUCCESS</td>
 *       <td>결제자 (시스템 알림)</td><td>PAYMENT</td></tr>
 *   <tr><td>{@code PaymentFailedEvent}</td><td>PAYMENT_FAILED</td>
 *       <td>결제자 (시스템 알림)</td><td>PAYMENT</td></tr>
 *   <tr><td>{@code CreatorApplicationApprovedEvent}</td><td>CREATOR_APPLICATION_APPROVED</td>
 *       <td>신청자 (시스템 알림)</td><td>CREATOR_APPLICATION</td></tr>
 *   <tr><td>{@code CreatorApplicationRejectedEvent}</td><td>CREATOR_APPLICATION_REJECTED</td>
 *       <td>신청자 (시스템 알림)</td><td>CREATOR_APPLICATION</td></tr>
 *   <tr><td>{@code PollVotedEvent}</td><td>POLL_VOTE</td>
 *       <td>투표를 만든 크리에이터</td><td>POLL</td></tr>
 * </table>
 *
 * <h2>아직 연결되지 않은 것</h2>
 * <ul>
 *   <li>{@code NEW_FEED} — 구독자 <b>전체</b>에게 보내는 팬아웃이라 성격이 다르다.
 *       {@code SubscriptionRepository} 에 구독자 ID 목록 조회가 없어 막혀 있고,
 *       대량 발송 배치 설계가 함께 필요하다</li>
 *   <li>{@code SUBSCRIPTION_EXPIRING} — 이벤트가 아니라 스케줄러 기반이 맞다
 *       ({@code SubscriptionSchedulingConfig})</li>
 * </ul>
 *
 * <h2>새 알림을 추가하려면</h2>
 * <ol>
 *   <li>도메인 쪽에 이벤트 {@code record} 정의 (네이밍 {@code {Aggregate}{과거분사}Event}).
 *       수신자를 발행 시점에 알 수 있으면 {@code recipientId} 를 이벤트에 담는다 —
 *       리스너가 다시 조회하지 않아도 된다</li>
 *   <li>이 패키지에 핸들러 추가. 위 3중 안전장치를 그대로 지킬 것</li>
 *   <li>알림 문구는 <b>리스너에서</b> 조립한다. 이벤트는 원재료(제목·미리보기·사유·금액)만 담는다</li>
 *   <li>시스템 알림(행위자가 없는 알림)은 {@code senderId = null}.
 *       수신자를 {@code senderId} 에도 넣으면 자기알림 억제에 걸려 <b>조용히 사라진다</b></li>
 *   <li>{@code NotificationType} 은 Flyway DDL 의 {@code ck_notification_type} CHECK 제약과
 *       반드시 일치해야 한다. 새 값은 마이그레이션이 선행되어야 하며,
 *       테스트는 H2 {@code create-drop} 이라 CHECK 를 검증하지 못한다 — 운영에서만 터진다</li>
 * </ol>
 */
package com.white.handdam.notification.event;
