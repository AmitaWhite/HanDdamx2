import { Link, NavLink } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Icon } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

const items = [
	{ to: paths.mypage, icon: "person", label: "프로필" },
	{ to: paths.feed, icon: "subscriptions", label: "구독 관리" },
	{ to: paths.dashboardProjects, icon: "category", label: "프로젝트 관리" },
	{ to: paths.dashboardPosts, icon: "article", label: "게시물 관리" },
];

/** 크리에이터 대시보드 좌측 사이드바 (데스크톱). 모바일은 하단 탭바로 대체. */
export function DashboardSidebar() {
	return (
		<aside className="hidden w-64 shrink-0 flex-col border-r border-outline-variant bg-surface-container-low md:flex">
			<div className="flex h-[72px] items-center px-6">
				<Link
					to={paths.landing}
					className="text-headline-md font-display font-bold text-primary"
				>
					한땀한땀
				</Link>
			</div>

			<div className="px-6 pb-4">
				<p className="text-caption font-caption uppercase tracking-wider text-secondary">
					Creator Dashboard
				</p>
			</div>

			<nav className="flex-1 px-3">
				<ul className="flex flex-col gap-1">
					{items.map((item) => (
						<li key={item.to}>
							<NavLink
								to={item.to}
								end
								className={({ isActive }) =>
									cn(
										"flex items-center gap-3 rounded px-3 py-2.5 text-label-md font-label-md transition-colors",
										isActive
											? "bg-primary/10 text-primary"
											: "text-on-surface hover:bg-surface-container",
									)
								}
							>
								<Icon name={item.icon} />
								{item.label}
							</NavLink>
						</li>
					))}
				</ul>
			</nav>

			<div className="m-3 flex items-center gap-3 rounded-xl bg-surface-container-lowest p-3">
				<Avatar size={40} />
				<div className="min-w-0">
					<p className="truncate text-label-md font-label-md text-on-surface">
						Artisan Kim
					</p>
					<p className="text-caption font-caption text-secondary">
						Pro Creator
					</p>
				</div>
			</div>
		</aside>
	);
}
