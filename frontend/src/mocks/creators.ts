export interface MockCreator {
	id: string;
	name: string;
	category: string;
	avatarSeed: string;
	coverSeed: string;
	subscriberCount: number;
	postCount: number;
	bio: string;
	hasQnaTab: boolean;
}

export const mockCreators: MockCreator[] = [
	/** 로컬 DB member id=9 (leelovery99 / 이동준) — /creators/9 메인 페이지용 */
	{
		id: "9",
		name: "이동준",
		category: "자수",
		avatarSeed: "artisan-dongjun",
		coverSeed: "cover-dongjun",
		subscriberCount: 1,
		postCount: 3,
		bio: "한땀한땀 — 손끝으로 이어가는 일상 공예. 구독자와 Q&A로 소통합니다.",
		hasQnaTab: true,
	},
	{
		id: "suyeon",
		name: "이수연",
		category: "자수",
		avatarSeed: "artisan-suyeon",
		coverSeed: "cover-suyeon",
		subscriberCount: 3240,
		postCount: 128,
		bio: "한 땀 한 땀, 계절의 들꽃을 자수로 기록합니다.",
		hasQnaTab: true,
	},
	{
		id: "seoyeon",
		name: "박서연",
		category: "자수",
		avatarSeed: "artisan-seoyeon",
		coverSeed: "cover-seoyeon",
		subscriberCount: 3240,
		postCount: 96,
		bio: "자수공방을 운영하며 프랑스자수를 가르칩니다.",
		hasQnaTab: true,
	},
	{
		id: "doyoon",
		name: "이도윤",
		category: "도자기",
		avatarSeed: "artisan-doyoon",
		coverSeed: "cover-doyoon",
		subscriberCount: 1890,
		postCount: 64,
		bio: "도자기 스튜디오에서 물레로 빚는 그릇들.",
		hasQnaTab: false,
	},
	{
		id: "minjae",
		name: "최민재",
		category: "가죽공예",
		avatarSeed: "artisan-minjae",
		coverSeed: "cover-minjae",
		subscriberCount: 2510,
		postCount: 97,
		bio: "가죽공방 온 — 손끝에서 태어나는 실용적인 아름다움.",
		hasQnaTab: true,
	},
	{
		id: "jiwoo",
		name: "한지우",
		category: "목공",
		avatarSeed: "artisan-jiwoo",
		coverSeed: "cover-jiwoo",
		subscriberCount: 1420,
		postCount: 52,
		bio: "원목가구 공작소, 나무결을 살리는 작업을 합니다.",
		hasQnaTab: false,
	},
	{
		id: "wood-boy",
		name: "목공소년",
		category: "목공",
		avatarSeed: "artisan-woodboy",
		coverSeed: "cover-woodboy",
		subscriberCount: 980,
		postCount: 41,
		bio: "월넛과 오크로 만드는 작은 소품들.",
		hasQnaTab: false,
	},
	{
		id: "ribbon-jo",
		name: "조리본",
		category: "자수",
		avatarSeed: "artisan-ribbonjo",
		coverSeed: "cover-ribbonjo",
		subscriberCount: 1120,
		postCount: 58,
		bio: "리본자수로 만드는 파우치와 소품.",
		hasQnaTab: false,
	},
	{
		id: "doye-kim",
		name: "김도예",
		category: "도자기",
		avatarSeed: "artisan-doyekim",
		coverSeed: "cover-doyekim",
		subscriberCount: 1560,
		postCount: 73,
		bio: "백자 달항아리를 빚는 도예가.",
		hasQnaTab: false,
	},
	{
		id: "leather-jung",
		name: "정가죽",
		category: "가죽공예",
		avatarSeed: "artisan-leatherjung",
		coverSeed: "cover-leatherjung",
		subscriberCount: 890,
		postCount: 34,
		bio: "식물성 염색 가죽으로 만드는 카드지갑.",
		hasQnaTab: false,
	},
	{
		id: "glass-yoon",
		name: "윤유리",
		category: "유리공예",
		avatarSeed: "artisan-glassyoon",
		coverSeed: "cover-glassyoon",
		subscriberCount: 670,
		postCount: 29,
		bio: "토치로 만드는 미니 유리병.",
		hasQnaTab: false,
	},
	{
		id: "calli-han",
		name: "한글씨",
		category: "캘리그라피",
		avatarSeed: "artisan-callihan",
		coverSeed: "cover-callihan",
		subscriberCount: 540,
		postCount: 22,
		bio: "붓펜으로 쓰는 사계절 문장.",
		hasQnaTab: false,
	},
];

export function findCreator(id: string): MockCreator {
	return mockCreators.find((c) => c.id === id) ?? mockCreators[0];
}
