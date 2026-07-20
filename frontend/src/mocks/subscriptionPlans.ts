export interface MockPlan {
	id: "free" | "paid";
	name: string;
	priceLabel: string;
	price: number;
	description?: string;
	benefits: string[];
	isRecommended?: boolean;
}

export const mockPlans: MockPlan[] = [
	{
		id: "free",
		name: "무료 플랜",
		priceLabel: "무료",
		price: 0,
		description: "주 1회 작업 사진 공유 및 공지사항 접근 권한",
		benefits: ["기본 혜택", "주요 공지 접근"],
	},
	{
		id: "paid",
		name: "유료 플랜",
		priceLabel: "9,900원 / 월",
		price: 9900,
		benefits: ["전체 작업 과정 영상 시청 (VOD)", "도안 파일 무제한 다운로드", "월간 창작 투표 2표 권한"],
		isRecommended: true,
	},
];
