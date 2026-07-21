import { paths } from "@/app/paths";
import { getPoll } from "@/features/poll/pollApi";
import type { NotificationResponse } from "./notificationsApi";

/**
 * referenceType/referenceId → 이동할 라우트.
 * POLL만 referenceId가 pollId라 게시물 라우팅을 위해 한 번 더 조회(poll.feedId)해야 해서 비동기.
 */
export async function resolveNotificationHref(n: NotificationResponse): Promise<string> {
	switch (n.referenceType) {
		case "CHAT_ROOM":
			// 채팅이 아직 단일 스레드라 room id 라우팅 자체가 없음 — 멀티룸 채팅 UI가 생기면 정리.
			return paths.chat;
		case "FEED":
			return paths.postDetail(n.referenceId);
		case "BOARD_POST":
			return paths.qnaPost(n.referenceId);
		case "PAYMENT":
			return paths.mypage;
		case "CREATOR_APPLICATION":
			// 승인/거절 모두 mypage로 — dashboardProjects는 CreatorOnlyRoute라 거절/미승인 사용자가 튕겨나감.
			return paths.mypage;
		case "POLL": {
			const poll = await getPoll(n.referenceId);
			return paths.postDetail(poll.feedId);
		}
		default:
			return paths.notifications;
	}
}
