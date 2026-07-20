import { paths } from "@/app/paths";

export type NotificationType =
	| "reply"
	| "payment"
	| "new_post"
	| "poll_vote"
	| "subscription_expiring"
	| "creator_approved";

export interface MockNotification {
	id: string;
	type: NotificationType;
	message: string;
	amount?: number;
	createdAtLabel: string;
	isRead: boolean;
	href: string;
}

/** 알림 타입 → 아이콘. 헤더 드롭다운과 전체 알림 페이지가 공유. */
export const NOTIFICATION_TYPE_ICON: Record<NotificationType, string> = {
	reply: "reply",
	payment: "payments",
	new_post: "photo_library",
	poll_vote: "how_to_vote",
	subscription_expiring: "schedule",
	creator_approved: "workspace_premium",
};

export const mockNotifications: MockNotification[] = [
	{
		id: "n1",
		type: "reply",
		message: "이수연님이 회원님의 댓글에 답글을 남겼어요.",
		createdAtLabel: "10분 전",
		isRead: false,
		href: paths.postDetail("post-1"),
	},
	{
		id: "n2",
		type: "payment",
		message: "이수연 작가 구독 결제가 완료되었습니다.",
		amount: 9900,
		createdAtLabel: "1시간 전",
		isRead: false,
		href: paths.mypage,
	},
	{
		id: "n3",
		type: "new_post",
		message: "구독 중인 목공소년님이 새 게시물을 업로드했어요.",
		createdAtLabel: "3시간 전",
		isRead: true,
		href: paths.postDetail("post-2"),
	},
	{
		id: "n4",
		type: "poll_vote",
		message: "참여하신 투표에 41명이 함께했어요.",
		createdAtLabel: "어제",
		isRead: true,
		href: paths.postDetail("post-1"),
	},
	{
		id: "n5",
		type: "subscription_expiring",
		message: "이수연 작가 구독이 3일 후 만료돼요.",
		createdAtLabel: "2일 전",
		isRead: false,
		href: paths.mypage,
	},
	{
		id: "n6",
		type: "creator_approved",
		message: "크리에이터 전환 신청이 승인되었습니다!",
		createdAtLabel: "3일 전",
		isRead: true,
		href: paths.dashboardProjects,
	},
];
