import { Outlet } from "react-router-dom";
import { BottomTabBar } from "@/components/nav/BottomTabBar";
import { DashboardSidebar } from "@/components/nav/DashboardSidebar";

/** 크리에이터 대시보드 레이아웃: 좌측 사이드바(데스크톱) + 본문. 소비자 글로벌 헤더와 분리. */
export function DashboardLayout() {
	return (
		<div className="flex min-h-screen bg-background">
			<DashboardSidebar />
			<main className="min-w-0 flex-1 pb-16 md:pb-0">
				<Outlet />
			</main>
			{/* 모바일에선 사이드바 대신 하단 탭바 사용 */}
			<BottomTabBar />
		</div>
	);
}
