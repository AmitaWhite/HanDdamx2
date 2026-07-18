import { Link, Outlet } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/features/auth/AuthContext";

/** 비로그인(랜딩/로그인/회원가입) 레이아웃: 미니멀 헤더 + 본문.
 *  로그인 상태로 랜딩 등에 진입해도 헤더가 로그인 상태를 반영한다. */
export function PublicLayout() {
	const { isAuthenticated, user } = useAuth();

	return (
		<div className="min-h-screen bg-background">
			<header className="fixed top-0 z-50 w-full border-b border-outline-variant bg-surface/95 backdrop-blur-[10px]">
				<nav className="container-page flex h-[72px] items-center justify-between">
					<Link
						to={paths.landing}
						className="text-headline-md font-display font-bold text-primary"
					>
						한땀한땀
					</Link>
					{isAuthenticated ? (
						<div className="flex items-center gap-3">
							<Link
								to={paths.home}
								className="text-label-md font-label-md text-on-surface hover:text-primary"
							>
								{user ? `${user.nickname}님` : "홈으로"}
							</Link>
							<Link to={paths.mypage} aria-label="내 프로필">
								<Avatar size={36} className="ring-1 ring-outline-variant" />
							</Link>
						</div>
					) : (
						<div className="flex items-center gap-3">
							<Link to={paths.login}>
								<Button variant="ghost" size="sm">
									로그인
								</Button>
							</Link>
							<Link to={paths.signup}>
								<Button variant="primary" size="sm">
									회원가입
								</Button>
							</Link>
						</div>
					)}
				</nav>
			</header>
			<main className="pt-[72px]">
				<Outlet />
			</main>
		</div>
	);
}
