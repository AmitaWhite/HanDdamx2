import { Link, NavLink } from "react-router-dom";
import { paths } from "@/app/paths";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import { cn } from "@/lib/cn";
import { NotificationDropdown } from "./NotificationDropdown";
import { ProfileMenu } from "./ProfileMenu";

/**
 * 상단 헤더 (72px, backdrop-blur). 로그인 여부에 따라 내용이 갈린다.
 * - 로그인: 좌측 네비+검색 + 채팅/알림 아이콘 + 프로필 메뉴
 * - 비로그인: 로고 + 로그인/회원가입 버튼만
 * PublicLayout/ConsumerLayout 양쪽에서 공용으로 사용.
 */
export function Header() {
	const { isAuthenticated } = useAuth();
	if (!isAuthenticated) return <GuestHeader />;
	return <AuthedHeader />;
}

function GuestHeader() {
	return (
		<header className="fixed top-0 z-50 w-full border-b border-outline-variant bg-surface/95 backdrop-blur-[10px]">
			<nav className="container-page flex h-[72px] items-center justify-between">
				<Link
					to={paths.landing}
					className="text-headline-md font-display font-bold text-primary"
				>
					한땀한땀
				</Link>
				<div className="flex items-center gap-3">
					<LinkButton to={paths.login} variant="ghost" size="sm">
						로그인
					</LinkButton>
					<LinkButton to={paths.signup} variant="primary" size="sm">
						회원가입
					</LinkButton>
				</div>
			</nav>
		</header>
	);
}

function AuthedHeader() {
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
					<NotificationDropdown />
					<ProfileMenu />
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
