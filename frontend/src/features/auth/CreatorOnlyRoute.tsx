import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { useAuth } from "./AuthContext";

/** 크리에이터가 아닌 로그인 사용자가 크리에이터 전용 화면에 접근하면 마이페이지로 돌려보낸다. */
export function CreatorOnlyRoute({ children }: { children: ReactNode }) {
	const { user } = useAuth();
	if (user?.role !== "CREATOR") return <Navigate to={paths.mypage} replace />;
	return <>{children}</>;
}
