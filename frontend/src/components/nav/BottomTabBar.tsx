import { NavLink } from "react-router-dom";
import { paths } from "@/app/paths";
import { Icon } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

const tabs = [
	{ to: paths.home, icon: "home", label: "홈" },
	{ to: paths.feed, icon: "subscriptions", label: "구독" },
	{ to: paths.dashboardPostNew, icon: "add_circle", label: "작성" },
	{ to: paths.notifications, icon: "notifications", label: "알림" },
	{ to: paths.mypage, icon: "person", label: "내정보" },
];

/** 모바일 전용 하단 탭바 (md 이상에선 숨김). 소비자 앱의 1차 네비게이션. */
export function BottomTabBar() {
	return (
		<nav className="fixed bottom-0 z-50 w-full border-t border-outline-variant bg-surface/95 backdrop-blur-[10px] md:hidden">
			<ul className="flex h-16 items-stretch">
				{tabs.map((tab) => (
					<li key={tab.to} className="flex-1">
						<NavLink
							to={tab.to}
							className={({ isActive }) =>
								cn(
									"flex h-full flex-col items-center justify-center gap-0.5 text-caption font-caption transition-colors",
									isActive ? "text-primary" : "text-secondary",
								)
							}
						>
							<Icon name={tab.icon} label={tab.label} />
							<span>{tab.label}</span>
						</NavLink>
					</li>
				))}
			</ul>
		</nav>
	);
}
