import type { ProjectStageId } from "./projects";

export interface MockPoll {
	question: string;
	options: { label: string; percentage: number }[];
	totalVotes: number;
	note?: string;
}

export interface MockPost {
	id: string;
	creatorId: string;
	projectId?: string;
	/** 프로젝트 진행 단계(있을 때만). 공예 종류(category)와는 다른 축. */
	stage?: ProjectStageId;
	category: string;
	title: string;
	excerpt: string;
	body?: string[];
	imageSeed: string;
	isPaid: boolean;
	likeCount: number;
	commentCount: number;
	voteCount?: number;
	createdAtLabel: string;
	poll?: MockPoll;
	tags?: string[];
}

export const mockPosts: MockPost[] = [
	{
		id: "post-1",
		creatorId: "suyeon",
		projectId: "proj-flower-wreath",
		stage: "production",
		category: "자수",
		title: "들꽃 리스, 마지막 스티치 배색 고민",
		excerpt:
			"전체적인 리스 형태는 잡혔는데, 가장 외곽에 들어갈 안개꽃 스티치 색상을 아이보리로 할지 완전 화이트로 할지 고민이에요.",
		body: [
			"전체적인 리스 형태는 잡혔는데, 가장 외곽에 들어갈 안개꽃 스티치 색상을 아이보리로 할지 완전 화이트로 할지 고민이에요.",
			"아이보리는 좀 더 따뜻하고 화이트는 또렷하게 대비가 되는 느낌이라 둘 다 매력이 있어서 구독자분들 의견을 들어보고 싶어요.",
		],
		imageSeed: "feed1",
		isPaid: true,
		likeCount: 128,
		commentCount: 12,
		voteCount: 41,
		createdAtLabel: "2026.07.02",
		poll: {
			question: "배색, 어떤 게 더 예쁠까요?",
			options: [
				{ label: "아이보리 톤", percentage: 64 },
				{ label: "완전 화이트", percentage: 36 },
			],
			totalVotes: 41,
			note: "유료 구독자는 2표가 반영됩니다.",
		},
		tags: ["자수", "프랑스자수"],
	},
	{
		id: "post-2",
		creatorId: "wood-boy",
		category: "목공",
		title: "월넛 트레이 샌딩 작업 중",
		excerpt: "220방부터 800방까지 순서대로 샌딩하고 있어요. 나뭇결이 살아나는 순간이 제일 좋습니다.",
		imageSeed: "feed2",
		isPaid: false,
		likeCount: 51,
		commentCount: 6,
		createdAtLabel: "2026.07.01",
		tags: ["목공"],
	},
	{
		id: "post-3",
		creatorId: "ribbon-jo",
		category: "자수",
		title: "리본자수 파우치 완성",
		excerpt: "실크 리본으로 입체감을 살린 장미 파우치가 드디어 완성됐어요.",
		imageSeed: "feed3",
		isPaid: false,
		likeCount: 42,
		commentCount: 4,
		createdAtLabel: "2026.06.29",
		tags: ["자수", "리본자수"],
	},
	{
		id: "post-4",
		creatorId: "doye-kim",
		category: "도자기",
		title: "백자 달항아리 초벌 전 정리",
		excerpt: "물레 성형 후 일주일 건조시킨 달항아리, 이제 초벌 소성 들어갑니다.",
		imageSeed: "feed4",
		isPaid: false,
		likeCount: 88,
		commentCount: 9,
		createdAtLabel: "2026.06.28",
		tags: ["도자기"],
	},
	{
		id: "post-5",
		creatorId: "leather-jung",
		category: "가죽공예",
		title: "카드지갑 코바 마감",
		excerpt: "식물성 염색 가죽 카드지갑, 가장자리 코바 작업으로 마무리했습니다.",
		imageSeed: "feed5",
		isPaid: true,
		likeCount: 63,
		commentCount: 7,
		createdAtLabel: "2026.06.27",
		tags: ["가죽공예"],
	},
	{
		id: "post-6",
		creatorId: "glass-yoon",
		category: "유리공예",
		title: "유리병 색 배합 실험",
		excerpt: "토치로 색유리를 녹여 미니 유리병에 그러데이션을 넣어봤어요.",
		imageSeed: "feed6",
		isPaid: false,
		likeCount: 37,
		commentCount: 3,
		createdAtLabel: "2026.06.25",
		tags: ["유리공예"],
	},
	{
		id: "post-8",
		creatorId: "suyeon",
		projectId: "proj-flower-wreath",
		stage: "prep",
		category: "자수",
		title: "들꽃 리스 도안 스케치",
		excerpt: "이번 리스에 들어갈 들꽃 배치를 종이에 먼저 그려봤어요.",
		imageSeed: "proj-flower-1",
		isPaid: false,
		likeCount: 64,
		commentCount: 5,
		createdAtLabel: "2026.06.20",
		tags: ["자수"],
	},
	{
		id: "post-9",
		creatorId: "suyeon",
		projectId: "proj-flower-wreath",
		stage: "prep",
		category: "자수",
		title: "실 색상 팔레트 구성",
		excerpt: "들꽃 리스에 쓸 DMC 실 24색을 골랐습니다.",
		imageSeed: "proj-flower-2",
		isPaid: false,
		likeCount: 58,
		commentCount: 3,
		createdAtLabel: "2026.06.15",
		tags: ["자수"],
	},
	{
		id: "post-10",
		creatorId: "suyeon",
		projectId: "proj-flower-wreath",
		stage: "concept",
		category: "자수",
		title: "들꽃 리스 연작, 구상 시작",
		excerpt: "이번 시즌엔 들꽃을 주제로 리스 3점을 연작으로 만들어보려 합니다.",
		imageSeed: "proj-flower-3",
		isPaid: false,
		likeCount: 112,
		commentCount: 14,
		createdAtLabel: "2026.03.05",
		tags: ["자수"],
	},
	{
		id: "post-11",
		creatorId: "suyeon",
		projectId: "proj-flower-wreath",
		stage: "production",
		category: "자수",
		title: "꽃잎 스티치 연습",
		excerpt: "렌치스티치와 랩드니들로 입체감 있는 꽃잎을 연습했어요.",
		imageSeed: "proj-flower-4",
		isPaid: true,
		likeCount: 95,
		commentCount: 8,
		createdAtLabel: "2026.06.25",
		tags: ["자수"],
	},
	{
		id: "post-12",
		creatorId: "doyoon",
		category: "도자기",
		title: "컵받침 유약 테스트",
		excerpt: "같은 태토에 유약만 다르게 발색 테스트를 했습니다.",
		imageSeed: "feed7",
		isPaid: false,
		likeCount: 34,
		commentCount: 2,
		createdAtLabel: "2026.06.10",
		tags: ["도자기"],
	},
	{
		id: "post-13",
		creatorId: "minjae",
		category: "가죽공예",
		title: "토트백 손바느질 마감",
		excerpt: "새들스티치로 튼튼하게 마감한 가죽 토트백입니다.",
		imageSeed: "feed8",
		isPaid: true,
		likeCount: 77,
		commentCount: 11,
		createdAtLabel: "2026.06.08",
		tags: ["가죽공예"],
	},
	{
		id: "post-14",
		creatorId: "jiwoo",
		category: "목공",
		title: "원목 스툴 다리 짜맞춤",
		excerpt: "못 없이 짜맞춤으로 스툴 다리를 조립했습니다.",
		imageSeed: "feed9",
		isPaid: false,
		likeCount: 66,
		commentCount: 5,
		createdAtLabel: "2026.06.05",
		tags: ["목공"],
	},
	{
		id: "post-15",
		creatorId: "calli-han",
		category: "캘리그라피",
		title: "여름 문장 붓펜 연습",
		excerpt: "장마철에 어울리는 문장을 붓펜으로 써봤어요.",
		imageSeed: "feed10",
		isPaid: false,
		likeCount: 48,
		commentCount: 4,
		createdAtLabel: "2026.06.03",
		tags: ["캘리그라피"],
	},
	{
		id: "post-16",
		creatorId: "seoyeon",
		category: "자수",
		title: "프랑스자수 초급반 커리큘럼 안내",
		excerpt: "다음 달부터 시작하는 초급반 커리큘럼을 공유합니다.",
		imageSeed: "feed11",
		isPaid: false,
		likeCount: 53,
		commentCount: 6,
		createdAtLabel: "2026.06.01",
		tags: ["자수"],
	},
];

export function findPost(id: string): MockPost {
	return mockPosts.find((p) => p.id === id) ?? mockPosts[0];
}
