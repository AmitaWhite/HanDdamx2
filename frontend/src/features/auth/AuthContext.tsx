import type { ReactNode } from "react";
import { createContext, useCallback, useContext, useState } from "react";
import { tokenStore } from "@/lib/api";
import { loginRequest, logoutRequest, refreshRequest } from "./authApi";
import type { AuthUser } from "./types";

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

export function AuthProvider({ children }: { children: ReactNode }) {
	const [accessToken, setAccessToken] = useState<string | null>(() =>
		tokenStore.get(),
	);
	const [user, setUser] = useState<AuthUser | null>(null);

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
		// user(nickname/role) 보강은 후속(/me 엔드포인트 또는 JWT 디코드). 지금은 토큰만으로 인증 처리.
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
