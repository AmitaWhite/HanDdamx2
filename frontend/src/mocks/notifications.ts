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
