import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { useAuth } from "./AuthContext";

/** 로그인하지 않은 사용자가 로그인 전용 화면에 접근하면 로그인 화면으로 돌려보낸다. */
export function AuthOnlyRoute({ children }: { children: ReactNode }) {
	const { isAuthenticated } = useAuth();
	if (!isAuthenticated) return <Navigate to={paths.login} replace />;
	return <>{children}</>;
}
