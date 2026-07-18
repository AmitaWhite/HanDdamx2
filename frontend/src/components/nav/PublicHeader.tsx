import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";

/** 비로그인 상태의 공개 페이지(랜딩/로그인/회원가입 등) 헤더. */
export function PublicHeader() {
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
	);
}
