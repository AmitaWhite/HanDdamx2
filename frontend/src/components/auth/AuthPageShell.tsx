import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

interface AuthPageShellProps {
	children: ReactNode;
	/** 세로 패딩(py-12) 포함 여부. 기본 true. 패딩 없는 레이아웃(OAuthCallbackPage 등)엔 false. */
	padded?: boolean;
	className?: string;
}

/** 인증 페이지(로그인/회원가입/이메일인증/구글콜백) 공통 최외곽 센터링 wrapper. */
export function AuthPageShell({
	children,
	padded = true,
	className,
}: AuthPageShellProps) {
	return (
		<div
			className={cn(
				"flex min-h-[calc(100vh-72px)] items-center justify-center px-margin-mobile",
				padded && "py-12",
				className,
			)}
		>
			{children}
		</div>
	);
}
