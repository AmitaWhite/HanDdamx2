/** project-detail(4단계 스테퍼)과 create-post(단계 선택)가 공유하는 단일 기준. */
export const PROJECT_STAGES = [
	{ id: "concept", ko: "구상/도안", en: "Concept" },
	{ id: "prep", ko: "재료 준비", en: "Preparation" },
	{ id: "production", ko: "제작", en: "Production" },
	{ id: "completion", ko: "마감/완성", en: "Completion" },
] as const;

export type ProjectStageId = (typeof PROJECT_STAGES)[number]["id"];

export interface MockProject {
	id: string;
	creatorId: string;
	title: string;
	status: "ongoing" | "completed";
	postCount: number;
	metaLabel: string;
	thumbnailSeed: string;
	periodLabel: string;
	currentStage: ProjectStageId;
}

export const mockProjects: MockProject[] = [
	{
		id: "proj-dongjun-stitch",
		creatorId: "9",
		title: "일상기록 자수 연작",
		status: "ongoing",
		postCount: 3,
		metaLabel: "업데이트 오늘",
		thumbnailSeed: "dongjun-proj1",
		periodLabel: "2026.07 ~ 진행중",
		currentStage: "production",
	},
	{
		id: "proj-dongjun-pouch",
		creatorId: "9",
		title: "미니 파우치 만들기",
		status: "ongoing",
		postCount: 1,
		metaLabel: "업데이트 3일 전",
		thumbnailSeed: "dongjun-proj2",
		periodLabel: "2026.06 ~ 진행중",
		currentStage: "prep",
	},
	{
		id: "proj-flower-wreath",
		creatorId: "suyeon",
		title: "들꽃 리스 연작",
		status: "ongoing",
		postCount: 12,
		metaLabel: "업데이트 2일 전",
		thumbnailSeed: "feed1",
		periodLabel: "2026.03 ~ 진행중",
		currentStage: "production",
	},
	{
		id: "proj-initial-hank",
		creatorId: "suyeon",
		title: "이니셜 손수건 클래스",
		status: "completed",
		postCount: 45,
		metaLabel: "수료생 45명",
		thumbnailSeed: "feed3",
		periodLabel: "2026.01 ~ 2026.02",
		currentStage: "completion",
	},
	{
		id: "proj-lavender",
		creatorId: "suyeon",
		title: "라벤더 파우치 만들기",
		status: "ongoing",
		postCount: 8,
		metaLabel: "업데이트 오늘",
		thumbnailSeed: "feed5",
		periodLabel: "2026.06 ~ 진행중",
		currentStage: "prep",
	},
	{
		id: "proj-winter-deco",
		creatorId: "suyeon",
		title: "겨울 홈데코 자수",
		status: "completed",
		postCount: 20,
		metaLabel: "지난 시즌",
		thumbnailSeed: "feed6",
		periodLabel: "2025.11 ~ 2026.01",
		currentStage: "completion",
	},
	{
		id: "proj-silk-thread",
		creatorId: "suyeon",
		title: "실크 스레드 실험",
		status: "ongoing",
		postCount: 5,
		metaLabel: "업데이트 3시간 전",
		thumbnailSeed: "feed4",
		periodLabel: "2026.07 ~ 진행중",
		currentStage: "concept",
	},
	{
		id: "proj-archive",
		creatorId: "suyeon",
		title: "첫 자수 아카이브",
		status: "completed",
		postCount: 31,
		metaLabel: "기록 보관",
		thumbnailSeed: "feed2",
		periodLabel: "2025.05 ~ 2025.10",
		currentStage: "completion",
	},
];

export function findProject(id: string): MockProject {
	return mockProjects.find((p) => p.id === id) ?? mockProjects[0];
}
