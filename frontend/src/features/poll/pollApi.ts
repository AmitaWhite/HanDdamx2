import { http, unwrap } from "@/lib/api";
import type { PollResponse, PollResultResponse } from "./types";

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
