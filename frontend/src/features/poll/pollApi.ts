import { http, unwrap } from "@/lib/api";

/** 백엔드 PollOptionResponse (com.white.handdam.poll.dto.response.PollOptionResponse) */
export interface PollOptionResponse {
	optionId: number;
	label: string;
	voteCount: number;
}

/**
 * 백엔드 PollResponse (com.white.handdam.poll.dto.response.PollResponse)
 * 알림의 POLL referenceType(=pollId)을 게시물 라우팅용 feedId로 풀기 위해 최소로 정의.
 */
export interface PollResponse {
	pollId: number;
	feedId: number;
	question: string;
	endAt: string;
	closed: boolean;
	active: boolean;
	options: PollOptionResponse[];
	myVotedOptionId: number | null;
}

/**
 * 투표 상세 조회.
 * 백엔드: GET /api/polls/{pollId}
 */
export function getPoll(pollId: number) {
	return unwrap<PollResponse>(http.get(`/polls/${pollId}`));
}
