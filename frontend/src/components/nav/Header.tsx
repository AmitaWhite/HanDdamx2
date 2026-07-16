import { Link, NavLink } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Icon } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

/** 소비자 앱 상단 헤더 (72px, backdrop-blur). 모바일에선 중앙 네비/검색을 숨기고 하단 탭바로 대체. */
export function Header() {
	return (
		<header className="fixed top-0 z-50 w-full border-b border-outline-variant bg-surface/95 backdrop-blur-[10px]">
			<nav className="container-page flex h-[72px] items-center justify-between">
				{/* 좌: 로고 + 데스크톱 네비 */}
				<div className="flex items-center gap-8">
					<Link
						to={paths.landing}
						className="text-headline-md font-display font-bold text-primary"
					>
						한땀한땀
					</Link>
					<div className="hidden items-center gap-6 md:flex">
						<HeaderLink to={paths.feed}>구독 피드</HeaderLink>
						<HeaderLink to={paths.home}>둘러보기</HeaderLink>
					</div>
				</div>

				{/* 중: 검색 (데스크톱만) */}
				<div className="mx-8 hidden max-w-md flex-1 lg:block">
					<div className="relative">
						<Icon
							name="search"
							className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-outline"
						/>
						<input
							className="w-full rounded-full border border-outline-variant bg-surface-container-low py-2 pl-10 pr-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
							placeholder="작가, 작품, 태그 검색"
							type="search"
						/>
					</div>
				</div>

				{/* 우: 아이콘 액션 */}
				<div className="flex items-center gap-1 sm:gap-2">
					<IconButton to={paths.chat} icon="chat_bubble" label="메시지" />
					<IconButton
						to={paths.notifications}
						icon="notifications"
						label="알림"
					/>
					<Link to={paths.mypage} aria-label="내 프로필" className="ml-1">
						<Avatar size={36} className="ring-1 ring-outline-variant" />
					</Link>
				</div>
			</nav>
		</header>
	);
}

function HeaderLink({
	to,
	children,
}: {
	to: string;
	children: React.ReactNode;
}) {
	return (
		<NavLink
			to={to}
			className={({ isActive }) =>
				cn(
					"text-label-md font-label-md font-medium transition-colors hover:text-primary",
					// 대비 개선: 기존 저대비 variant 대신 on-surface 사용
					isActive ? "text-primary" : "text-on-surface",
				)
			}
		>
			{children}
		</NavLink>
	);
}

function IconButton({
	to,
	icon,
	label,
}: {
	to: string;
	icon: string;
	label: string;
}) {
	return (
		<Link
			to={to}
			aria-label={label}
			className="flex h-10 w-10 items-center justify-center rounded-full text-on-surface transition-colors hover:bg-surface-container-low"
		>
			<Icon name={icon} label={label} />
		</Link>
	);
}
