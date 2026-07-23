import { http, unwrap, unwrapVoid } from "@/lib/api";
import type { PollResponse, PollResultResponse } from "./types";

export interface UpdatePollParams {
	/** question, endAt 중 최소 하나는 필요 */
	question?: string;
	/** ISO 8601 문자열 (Instant) */
	endAt?: string;
}

export interface CreatePollParams {
	/** ISO 8601 문자열 (Instant) */
	endAt: string;
	question: string;
	/** 2~10개 */
	options: string[];
}

/**
 * 피드에 투표 추가 (LYJ-022).
 * 백엔드: POST /api/feeds/{feedId}/poll
 * 권한: 피드 작성자(프로젝트 소유 크리에이터)
 */
export function createFeedPoll(feedId: number, params: CreatePollParams) {
	return unwrap<number>(http.post(`/feeds/${feedId}/poll`, params));
}

/**
 * 투표 선택지·내 참여 조회 (LYJ-023).
 * 백엔드: GET /api/polls/{pollId}
 * 권한: 전체 (비로그인 포함)
 */
export function getPoll(pollId: number) {
	return unwrap<PollResponse>(http.get(`/polls/${pollId}`));
}

/**
 * 투표 질문·종료일 수정 (LYJ-024).
 * 백엔드: PATCH /api/polls/{pollId}
 * 권한: 투표를 만든 피드 작성자
 */
export function updatePoll(pollId: number, params: UpdatePollParams) {
	return unwrap<number>(http.patch(`/polls/${pollId}`, params));
}

/**
 * 투표 삭제 (LYJ-025).
 * 백엔드: DELETE /api/polls/{pollId}
 * 권한: 투표를 만든 피드 작성자
 */
export function deletePoll(pollId: number) {
	return unwrapVoid(http.delete(`/polls/${pollId}`));
}

/**
 * 투표 참여 (LYJ-026).
 * 백엔드: POST /api/polls/{pollId}/votes
 */
export function votePoll(pollId: number, optionId: number) {
	return unwrap<number>(http.post(`/polls/${pollId}/votes`, { optionId }));
}

/**
 * 내 투표 선택지 변경 (LYJ-027).
 * 백엔드: PATCH /api/polls/{pollId}/votes/me
 */
export function changePollVote(pollId: number, optionId: number) {
	return unwrap<number>(http.patch(`/polls/${pollId}/votes/me`, { optionId }));
}

/**
 * 투표 결과 조회 (LYJ-028).
 * 백엔드: GET /api/polls/{pollId}/results
 * 권한: 전체 (비로그인 포함)
 */
export function getPollResults(pollId: number) {
	return unwrap<PollResultResponse>(http.get(`/polls/${pollId}/results`));
}

/**
 * 투표 조기 종료 (LYJ-029).
 * 백엔드: PATCH /api/polls/{pollId}/close
 * 권한: 투표를 만든 피드 작성자
 */
export function closePoll(pollId: number) {
	return unwrap<number>(http.patch(`/polls/${pollId}/close`, {}));
}
