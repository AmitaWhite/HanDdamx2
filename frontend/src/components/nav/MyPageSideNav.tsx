import { NavLink } from "react-router-dom";
import { paths } from "@/app/paths";
import { Icon } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

const NAV_ITEMS = [
	{ label: "프로필", icon: "person", to: paths.mypage },
	{ label: "프로필 수정", icon: "settings", to: paths.mypageSettings },
] as const;

/** 아직 화면이 없는 항목 — 준비 중으로 표기(가짜 경로로 연결하지 않음). */
const COMING_SOON_ITEMS = [
	{ label: "구독 내역", icon: "subscriptions" },
	{ label: "결제 내역", icon: "payments" },
	{ label: "댓글 내역", icon: "comment" },
] as const;

/** 마이페이지/프로필 설정 공용 좌측 사이드 메뉴. */
export function MyPageSideNav() {
	return (
		<aside className="hidden w-64 shrink-0 flex-col gap-1 md:flex">
			<div className="mb-4">
				<p className="text-headline-md font-display text-on-surface">마이페이지</p>
				<p className="text-caption font-caption text-secondary">내 활동을 관리하세요</p>
			</div>
			{NAV_ITEMS.map((item) => (
				<NavLink
					key={item.to}
					to={item.to}
					end
					className={({ isActive }) =>
						cn(
							"flex items-center gap-3 rounded-lg px-3 py-2.5 text-label-md font-label-md transition-colors",
							isActive ? "bg-primary/10 text-primary" : "text-on-surface hover:bg-surface-container-low",
						)
					}
				>
					<Icon name={item.icon} />
					{item.label}
				</NavLink>
			))}
			{COMING_SOON_ITEMS.map((item) => (
				<div
					key={item.label}
					className="flex items-center gap-3 rounded-lg px-3 py-2.5 text-label-md font-label-md text-outline"
				>
					<Icon name={item.icon} />
					{item.label}
				</div>
			))}
			<button
				type="button"
				className="mt-4 flex items-center justify-center gap-2 rounded-lg bg-primary-container py-3 text-label-md font-label-md text-on-primary-container transition-opacity hover:opacity-90"
			>
				<Icon name="swap_horiz" />
				크리에이터 전환
			</button>
		</aside>
	);
}
