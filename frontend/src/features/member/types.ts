import type { Role } from "@/features/auth/types";

export type OAuthProvider = "NONE" | "GOOGLE";

export interface MemberProfileResponse {
	id: number;
	email: string;
	nickname: string;
	profileImageUrl: string | null;
	role: Role;
	oauthProvider: OAuthProvider;
	createdAt: string;
}

export interface MemberSummaryResponse {
	id: number;
	nickname: string;
	profileImageUrl: string | null;
	role: Role;
	introduction: string | null;
	myBoardCommentCount: number;
	subscribingCount: number;
}