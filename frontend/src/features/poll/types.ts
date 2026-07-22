/** 백엔드 PollOptionResponse */
export interface PollOptionResponse {
	optionId: number;
	optionText: string;
	orderIndex: number;
}

/** 백엔드 PollResponse */
export interface PollResponse {
	pollId: number;
	feedId: number;
	question: string;
	endAt: string;
	closed: boolean;
	active: boolean;
	options: PollOptionResponse[];
	/** 내가 투표한 선택지 id. 미투표면 null */
	myVotedOptionId: number | null;
}

/** 백엔드 PollResultResponse.OptionResult */
export interface PollOptionResult {
	optionId: number;
	optionText: string;
	orderIndex: number;
	weight: number;
}

/** 백엔드 PollResultResponse */
export interface PollResultResponse {
	pollId: number;
	question: string;
	active: boolean;
	totalWeight: number;
	options: PollOptionResult[];
}
