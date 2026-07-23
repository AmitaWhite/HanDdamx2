package com.white.handdam.poll.event;

/**
 * 투표에 참여했을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code PollNotificationListener} 가 구독해
 * 투표를 만든 크리에이터에게 {@code POLL_VOTE} 알림을 만든다.
 *
 * <h3>발행 위치 — try 블록 안</h3>
 * {@code poll/service/PollService#vote} — {@code @Transactional} 안,
 * 투표 저장 직후 (현재 184번 줄 부근).
 *
 * <p>현재 코드는 {@code return pollVoteRepository.save(vote).getId();} 로
 * save 가 return 문에 인라인되어 있고 {@code DataIntegrityViolationException} 을 잡는
 * try/catch 안에 있다. <b>지역 변수를 추출하고 try 블록 안에서 발행</b>해야 한다
 * (catch 밖에서 발행하면 중복 투표 예외 경로에서도 알림이 나간다).
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * try {
 *     PollVote saved = pollVoteRepository.save(vote);
 *     eventPublisher.publishEvent(new PollVotedEvent(
 *         pollId,                    // 파라미터
 *         poll.getFeedId(),
 *         option.getId(),
 *         memberId,                  // 파라미터 (투표자)
 *         project.getCreatorId()     // 이미 로드된 project (161번 줄)
 *     ));
 *     return saved.getId();
 * } catch (DataIntegrityViolationException e) {
 *     throw new CustomException(PollErrorCode.POLL_ALREADY_VOTED);
 * }
 * }</pre>
 *
 * @param pollId      투표 ID (알림 클릭 시 이동 대상)
 * @param feedId      투표가 속한 피드
 * @param optionId    선택한 항목
 * @param actorId     투표한 사람
 * @param recipientId 알림 수신자 = 투표를 만든 크리에이터.
 *                    크리에이터가 자기 투표에 참여하면 알림은 자동으로 억제된다.
 */
public record PollVotedEvent(
	Long pollId,
	Long feedId,
	Long optionId,
	Long actorId,
	Long recipientId
) {
}
