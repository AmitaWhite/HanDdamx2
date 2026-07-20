import { Outlet } from "react-router-dom";
import { Header } from "@/components/nav/Header";

/**
 * 비로그인 전용 페이지(랜딩/로그인/회원가입/이메일인증 등)의 레이아웃.
 * Header가 로그인 여부에 따라 자체적으로 분기하므로 그대로 재사용한다.
 */
export function PublicLayout() {
	return (
		<div className="min-h-screen bg-background">
			<Header />
			<main className="pt-[72px]">
				<Outlet />
			</main>
		</div>
	);
}
