import type { ReactNode } from "react";
import { MyPageSideNav } from "./MyPageSideNav";

/** 마이페이지/프로필 설정 공용 레이아웃: 사이드 메뉴 + 컨텐츠. */
interface MyPageShellProps {
  children: ReactNode;
  onCreatorApply?: () => void;
}

export function MyPageShell({ children, onCreatorApply }: MyPageShellProps) {
	return (
		<div className="container-page flex gap-10 py-8">
      <MyPageSideNav onCreatorApply={onCreatorApply} />
			<div className="min-w-0 flex-1">{children}</div>
		</div>
	);
}
