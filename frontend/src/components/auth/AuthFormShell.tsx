import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

const DEFAULT_SUBTITLE = "공예 작가와 팬을 잇는 구독 플랫폼";

interface AuthFormShellProps {
	/** "한땀한땀" | "회원가입" 등 페이지별 타이틀 */
	title: string;
	subtitle?: string;
	children: ReactNode;
	className?: string;
}

/** 브랜드 헤더(타이틀+태그라인) + max-w-[440px] 세로 컬럼. 로그인/회원가입 폼에서 공용. */
export function AuthFormShell({
	title,
	subtitle = DEFAULT_SUBTITLE,
	children,
	className,
}: AuthFormShellProps) {
	return (
		<div
			className={cn(
				"flex w-full max-w-[440px] flex-col items-center",
				className,
			)}
		>
			<div className="mb-10 text-center">
				<h1 className="mb-2 text-headline-lg font-display text-on-surface">
					{title}
				</h1>
				{subtitle && <p className="text-body-md text-secondary">{subtitle}</p>}
			</div>
			{children}
		</div>
	);
}
