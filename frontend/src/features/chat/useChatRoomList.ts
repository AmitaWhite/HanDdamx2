import { useEffect, useState } from "react";
import { ApiError } from "@/lib/api";
import { getMemberPublicProfile } from "@/features/member/memberApi";
import {
	type ChatRoomListItemResponse,
	getMyChatRooms,
	isChatRoomWithMessages,
	resolveChatOpponentId,
} from "./chatApi";
import type { OpponentProfile } from "./useChatOpponent";

interface UseChatRoomListResult {
	rooms: ChatRoomListItemResponse[];
	profiles: Map<number, OpponentProfile>;
	loading: boolean;
	error: string | null;
}

/**
 * 내 채팅방 목록과 상대방 프로필을 한 번에 로드한다.
 *
 * <ul>
 *   <li>메시지가 하나도 없는 방(빈 방)은 제외</li>
 *   <li>상대방 프로필은 실패해도 fallback 항목(회원 #id)을 넣어 UI 가 깨지지 않는다</li>
 * </ul>
 */
export function useChatRoomList(myMemberId: number | null): UseChatRoomListResult {
	const [rooms, setRooms] = useState<ChatRoomListItemResponse[]>([]);
	const [profiles, setProfiles] = useState<Map<number, OpponentProfile>>(
		new Map(),
	);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		if (myMemberId == null) return;

		let cancelled = false;
		setLoading(true);
		setError(null);

		getMyChatRooms()
			.then(async (data) => {
				if (cancelled) return;
				const withMessages = data.filter(isChatRoomWithMessages);
				setRooms(withMessages);

				const opponentIds = [
					...new Set(
						withMessages.map((room) =>
							resolveChatOpponentId(room, myMemberId),
						),
					),
				];
				const entries = await Promise.all(
					opponentIds.map(async (memberId) => {
						try {
							const profile = await getMemberPublicProfile(memberId);
							return [
								memberId,
								{
									memberId: profile.id,
									nickname: profile.nickname,
									profileImageUrl: profile.profileImageUrl,
									isCreator: profile.isCreator,
								},
							] as const;
						} catch {
							return [
								memberId,
								{
									memberId,
									nickname: `회원 #${memberId}`,
									profileImageUrl: null,
									isCreator: false,
								},
							] as const;
						}
					}),
				);
				if (!cancelled) setProfiles(new Map(entries));
			})
			.catch((err: unknown) => {
				if (cancelled) return;
				setError(
					err instanceof ApiError
						? err.message
						: "채팅 목록을 불러오지 못했습니다.",
				);
				setRooms([]);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		return () => {
			cancelled = true;
		};
	}, [myMemberId]);

	return { rooms, profiles, loading, error };
}
