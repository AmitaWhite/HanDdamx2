import type { ReactNode } from "react";
import { createContext, useCallback, useContext, useEffect, useState } from "react";
import { decodeJwtPayload, tokenStore } from "@/lib/api";
import { getMyProfile } from "@/features/member/memberApi";
import { loginRequest, logoutRequest, refreshRequest } from "./authApi";
import type { AuthUser, Role } from "./types";

interface AuthContextValue {
	accessToken: string | null;
	user: AuthUser | null;
	isAuthenticated: boolean;
	/** 이메일/비밀번호 로그인. 실패 시 ApiError throw */
	login: (email: string, password: string) => Promise<void>;
	/** 구글 OAuth 콜백에서 refresh 쿠키로 access token 확보 */
	completeOAuth: () => Promise<void>;
	logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * accessToken(JWT)의 sub/role 클레임에서 memberId/role을 동기적으로 복원.
 * 닉네임은 JWT에 없어 여기선 채우지 않는다 — 표시용이라 비동기(getMyProfile)로 나중에 보강.
 */
function userFromToken(token: string): AuthUser | null {
	const claims = decodeJwtPayload(token);
	if (!claims?.sub || !claims.role) return null;
	const memberId = Number(claims.sub);
	if (Number.isNaN(memberId)) return null;
	return { memberId, role: claims.role as Role };
}

export function AuthProvider({ children }: { children: ReactNode }) {
	const [accessToken, setAccessToken] = useState<string | null>(() =>
		tokenStore.get(),
	);
	const [user, setUser] = useState<AuthUser | null>(() => {
		const token = tokenStore.get();
		return token ? userFromToken(token) : null;
	});

	// 닉네임(표시용)만 비동기로 보강 — 실패해도 role 기반 판단엔 영향 없어 조용히 무시한다.
	useEffect(() => {
		if (!accessToken) return;
		getMyProfile()
			.then((profile) => {
				setUser((prev) => (prev ? { ...prev, nickname: profile.nickname } : prev));
			})
			.catch(() => {});
	}, [accessToken]);

	const login = useCallback(async (email: string, password: string) => {
		const res = await loginRequest(email, password);
		tokenStore.set(res.accessToken);
		setAccessToken(res.accessToken);
		setUser({ memberId: res.memberId, nickname: res.nickname, role: res.role });
	}, []);

	const completeOAuth = useCallback(async () => {
		const res = await refreshRequest();
		tokenStore.set(res.accessToken);
		setAccessToken(res.accessToken);
		setUser(userFromToken(res.accessToken));
		// 닉네임은 위 useEffect(accessToken 변경 감지)가 채워준다.
	}, []);

	const logout = useCallback(async () => {
		try {
			await logoutRequest();
		} finally {
			tokenStore.clear();
			setAccessToken(null);
			setUser(null);
		}
	}, []);

	const value: AuthContextValue = {
		accessToken,
		user,
		isAuthenticated: !!accessToken,
		login,
		completeOAuth,
		logout,
	};

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
	const ctx = useContext(AuthContext);
	if (!ctx)
		throw new Error("useAuth 는 AuthProvider 안에서만 사용할 수 있습니다.");
	return ctx;
}
