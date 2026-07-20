import { Outlet } from "react-router-dom";
import { BottomTabBar } from "@/components/nav/BottomTabBar";
import { Header } from "@/components/nav/Header";
import { useAuth } from "@/features/auth/AuthContext";

/** 소비자 앱 레이아웃: 상단 헤더 + 하단 탭바(모바일, 로그인 시에만). 본문은 헤더/탭바 높이만큼 패딩. */
export function ConsumerLayout() {
	const { isAuthenticated } = useAuth();

	return (
		<div className="min-h-screen bg-background">
			<Header />
			{/* 상단 72px, 하단 탭바 64px(모바일) 만큼 여백 확보 */}
			<main className="pt-[72px] pb-16 md:pb-0">
				<Outlet />
			</main>
			{isAuthenticated && <BottomTabBar />}
		</div>
	);
}
