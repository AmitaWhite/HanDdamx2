import { Link, Outlet } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";

/** 비로그인(랜딩/로그인/회원가입) 레이아웃: 미니멀 헤더 + 본문. */
export function PublicLayout() {
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
				</nav>
			</header>
			<main className="pt-[72px]">
				<Outlet />
			</main>
		</div>
	);
}
