import { Outlet } from "react-router-dom";
import { Header } from "@/components/nav/Header";
import { PublicHeader } from "@/components/nav/PublicHeader";
import { useAuth } from "@/features/auth/AuthContext";

/**
 * 비로그인 전용 페이지(랜딩/로그인/회원가입/이메일인증 등)의 레이아웃.
 * 로그인 상태로 진입하면 소비자 앱과 동일한 헤더(Header)를 그대로 재사용한다.
 */
export function PublicLayout() {
	const { isAuthenticated } = useAuth();

	return (
		<div className="min-h-screen bg-background">
			{isAuthenticated ? <Header /> : <PublicHeader />}
			<main className="pt-[72px]">
				<Outlet />
			</main>
		</div>
	);
}
