import type { HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

interface ChipProps extends HTMLAttributes<HTMLSpanElement> {
	/** 활성/선택 상태면 브랜드 레드 채움 */
	active?: boolean;
	/** md: 카테고리/필터 칩(기본). sm: 이미지 위 오버레이·인라인 상태 뱃지용 축소형. */
	size?: "sm" | "md";
}

/** 카테고리/태그 칩이자 상태 뱃지. 기본은 연회색, active 시 브랜드 레드. */
export function Chip({ active, size = "md", className, ...props }: ChipProps) {
	return (
		<span
			className={cn(
				"inline-block",
				size === "sm" ? "rounded px-2 py-1 text-[10px] font-bold" : "rounded-full px-3 py-1 text-caption font-caption",
				active
					? "bg-primary text-on-primary"
					: "bg-surface-container text-secondary",
				className,
			)}
			{...props}
		/>
	);
}
