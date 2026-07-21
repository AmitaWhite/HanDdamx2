import { useEffect, useState } from "react";
import { getMemberPublicProfile } from "@/features/member/memberApi";

/** 채팅 상대방 표시용 프로필 */
export interface OpponentProfile {
	memberId: number;
	nickname: string;
	profileImageUrl: string | null;
	isCreator: boolean;
}

/**
 * 상대방 공개 프로필을 조회한다. 실패 시 null 반환 — 호출부에서 mock fallback 처리.
 */
export function useChatOpponent(opponentMemberId: number): OpponentProfile | null {
	const [opponent, setOpponent] = useState<OpponentProfile | null>(null);

	useEffect(() => {
		let cancelled = false;
		setOpponent(null);

		getMemberPublicProfile(opponentMemberId)
			.then((profile) => {
				if (cancelled) return;
				setOpponent({
					memberId: profile.id,
					nickname: profile.nickname,
					profileImageUrl: profile.profileImageUrl,
					isCreator: profile.isCreator,
				});
			})
			.catch(() => {
				// 조회 실패는 조용히 무시 — 호출부의 fallback (닉네임 · 아바타)이 노출된다.
			});

		return () => {
			cancelled = true;
		};
	}, [opponentMemberId]);

	return opponent;
}
