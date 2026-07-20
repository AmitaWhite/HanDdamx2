export interface MockComment {
	id: string;
	postId: string;
	authorName: string;
	avatarSeed: string;
	isPaidSubscriber: boolean;
	body: string;
	createdAtLabel: string;
}

/** 일반 게시물·Q&A 게시글 공용. postId로 필터링해서 사용. */
export const mockComments: MockComment[] = [
	{
		id: "c1",
		postId: "post-1",
		authorName: "공예덕후",
		avatarSeed: "member1",
		isPaidSubscriber: true,
		body: "저는 아이보리 톤이 더 자연스러울 것 같아요!",
		createdAtLabel: "1시간 전",
	},
	{
		id: "c2",
		postId: "post-1",
		authorName: "실뜨개는즐거워",
		avatarSeed: "member2",
		isPaidSubscriber: false,
		body: "완전 화이트로 하면 더 깔끔할 것 같아요.",
		createdAtLabel: "2시간 전",
	},
	{
		id: "c3",
		postId: "post-1",
		authorName: "들꽃사랑",
		avatarSeed: "member3",
		isPaidSubscriber: true,
		body: "다음 리스도 기대할게요 :)",
		createdAtLabel: "3시간 전",
	},
	{
		id: "c4",
		postId: "post-1",
		authorName: "초보자수러",
		avatarSeed: "member4",
		isPaidSubscriber: false,
		body: "배색 고민하는 과정도 너무 유익해요!",
		createdAtLabel: "5시간 전",
	},
	{
		id: "c5",
		postId: "qna-1",
		authorName: "이수연",
		avatarSeed: "artisan-suyeon",
		isPaidSubscriber: false,
		body: "매듭이 자꾸 풀린다면 실을 두 바퀴 감아보세요. 훨씬 단단해져요!",
		createdAtLabel: "1일 전",
	},
	{
		id: "c6",
		postId: "qna-1",
		authorName: "새싹공예가",
		avatarSeed: "member5",
		isPaidSubscriber: true,
		body: "저도 궁금했던 팁이에요, 감사합니다!",
		createdAtLabel: "20시간 전",
	},
	{
		id: "c7",
		postId: "post-2",
		authorName: "이수연",
		avatarSeed: "artisan-suyeon",
		isPaidSubscriber: false,
		body: "샌딩 방향이 결을 따라 예쁘게 잡혔네요!",
		createdAtLabel: "2026.03.14 15:30",
	},
	{
		id: "c8",
		postId: "post-4",
		authorName: "이수연",
		avatarSeed: "artisan-suyeon",
		isPaidSubscriber: false,
		body: "백자 색감이 정말 곱네요, 초벌 잘 나오길 바라요.",
		createdAtLabel: "2026.03.10 11:02",
	},
];

export function commentsFor(postId: string): MockComment[] {
	return mockComments.filter((c) => c.postId === postId);
}
