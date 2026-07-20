import type { ReactNode } from "react";
import { MyPageSideNav } from "./MyPageSideNav";

/** 마이페이지/프로필 설정 공용 레이아웃: 사이드 메뉴 + 컨텐츠. */
export function MyPageShell({ children }: { children: ReactNode }) {
	return (
		<div className="container-page flex gap-10 py-8">
			<MyPageSideNav />
			<div className="min-w-0 flex-1">{children}</div>
		</div>
	);
}
