import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { useAuth } from "./AuthContext";

/** 이미 로그인된 사용자가 로그인/회원가입 화면에 접근하면 홈으로 돌려보낸다. */
export function GuestOnlyRoute({ children }: { children: ReactNode }) {
	const { isAuthenticated } = useAuth();
	if (isAuthenticated) return <Navigate to={paths.home} replace />;
	return <>{children}</>;
}
