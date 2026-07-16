import type { HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

interface ChipProps extends HTMLAttributes<HTMLSpanElement> {
	/** 활성/선택 상태면 브랜드 레드 채움 */
	active?: boolean;
}

/** 카테고리/태그 칩. 기본은 연회색, active 시 브랜드 레드. */
export function Chip({ active, className, ...props }: ChipProps) {
	return (
		<span
			className={cn(
				"inline-block rounded-full px-3 py-1 text-caption font-caption",
				active
					? "bg-primary text-on-primary"
					: "bg-surface-container text-secondary",
				className,
			)}
			{...props}
		/>
	);
}
