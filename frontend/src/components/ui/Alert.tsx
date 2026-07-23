import type { HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

interface AlertProps extends HTMLAttributes<HTMLParagraphElement> {
	/** 지금은 error만 필요 — 다른 variant는 실제 필요해질 때 추가 */
	variant?: "error";
}

/** 폼 제출 실패 등 서버 에러를 알리는 인라인 배너. */
export function Alert({ variant = "error", className, ...props }: AlertProps) {
	return (
		<p
			role="alert"
			className={cn(
				"rounded px-4 py-3 text-label-md font-label-md",
				variant === "error" && "bg-error-container text-on-error-container",
				className,
			)}
			{...props}
		/>
	);
}
