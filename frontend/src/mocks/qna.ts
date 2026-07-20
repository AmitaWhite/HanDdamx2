export type QnaCategory = "제작 질문" | "작품 피드백" | "콘텐츠 제안" | "재료 추천" | "일반 소통";
export type QnaStatus = "answered" | "pending";

export interface MockQnaPost {
	id: string;
	creatorId: string;
	category: QnaCategory;
	status: QnaStatus;
	title: string;
	authorName: string;
	commentCount: number;
	createdAtLabel: string;
	body: string[];
	attachedImageSeeds?: string[];
}

export interface MockQnaAnswer {
	creatorName: string;
	avatarSeed: string;
	body: string[];
	answeredAtLabel: string;
}

export const mockQnaPosts: MockQnaPost[] = [
	{
		id: "qna-1",
		creatorId: "suyeon",
		category: "제작 질문",
		status: "answered",
		title: "프랑스자수 매듭 스티치 팁이 궁금해요",
		authorName: "매듭실패자",
		commentCount: 2,
		createdAtLabel: "2일 전",
		body: [
			"프렌치노트를 뜰 때마다 자꾸 실이 엉키고 매듭이 커져버려요.",
			"실을 감는 횟수나 바늘을 빼는 속도에 요령이 있을까요?",
		],
		attachedImageSeeds: ["qna-1-a", "qna-1-b"],
	},
	{
		id: "qna-2",
		creatorId: "suyeon",
		category: "작품 피드백",
		status: "pending",
		title: "제가 만든 리스 사진인데 피드백 부탁드려요",
		authorName: "들꽃사랑",
		commentCount: 1,
		createdAtLabel: "3일 전",
		body: ["처음으로 완성한 들꽃 리스예요. 배색이나 스티치 밀도가 괜찮은지 봐주시면 감사하겠습니다!"],
		attachedImageSeeds: ["qna-2-a"],
	},
	{
		id: "qna-3",
		creatorId: "suyeon",
		category: "재료 추천",
		status: "answered",
		title: "DMC 실 추천해주실 수 있나요?",
		authorName: "실뜨개는즐거워",
		commentCount: 5,
		createdAtLabel: "4일 전",
		body: ["파스텔 톤 들꽃 자수를 시작하려는데, 어떤 색상 조합의 DMC 실 세트를 추천하시나요?"],
	},
	{
		id: "qna-4",
		creatorId: "suyeon",
		category: "콘텐츠 제안",
		status: "pending",
		title: "가방 자수 튜토리얼도 올려주세요!",
		authorName: "공예덕후",
		commentCount: 0,
		createdAtLabel: "5일 전",
		body: ["에코백에 놓는 자수 튜토리얼도 보고 싶어요. 다음 프로젝트로 고려해주실 수 있을까요?"],
	},
	{
		id: "qna-5",
		creatorId: "suyeon",
		category: "일반 소통",
		status: "answered",
		title: "작업실은 어디에 있으신가요?",
		authorName: "새싹공예가",
		commentCount: 2,
		createdAtLabel: "1주 전",
		body: ["언젠가 원데이클래스가 열리면 참여하고 싶어서요. 작업실 위치가 궁금합니다!"],
	},
	{
		id: "qna-6",
		creatorId: "suyeon",
		category: "제작 질문",
		status: "pending",
		title: "실이 자꾸 엉켜요, 팁 있을까요?",
		authorName: "매듭실패자",
		commentCount: 0,
		createdAtLabel: "1주 전",
		body: ["작업 중간에 실이 계속 꼬여서 자꾸 끊어먹게 됩니다. 예방하는 방법이 있을까요?"],
	},
];

export const mockQnaAnswers: Record<string, MockQnaAnswer> = {
	"qna-1": {
		creatorName: "이수연",
		avatarSeed: "artisan-suyeon",
		body: [
			"저도 처음엔 매듭이 자꾸 커져서 고생했어요!",
			"실을 바늘에 두 바퀴만 감고, 뺄 때 실을 팽팽하게 당긴 채로 천천히 빼시면 훨씬 깔끔하게 나옵니다.",
		],
		answeredAtLabel: "1일 전",
	},
	"qna-3": {
		creatorName: "이수연",
		avatarSeed: "artisan-suyeon",
		body: ["파스텔 들꽃엔 DMC 3078, 3689, 3813 조합을 자주 씁니다. 은은하게 잘 어울려요."],
		answeredAtLabel: "3일 전",
	},
	"qna-5": {
		creatorName: "이수연",
		avatarSeed: "artisan-suyeon",
		body: ["서울 마포구에 작업실이 있어요. 원데이클래스 일정 열리면 공지로 먼저 안내드릴게요!"],
		answeredAtLabel: "6일 전",
	},
};

export function findQnaPost(id: string): MockQnaPost {
	return mockQnaPosts.find((q) => q.id === id) ?? mockQnaPosts[0];
}
