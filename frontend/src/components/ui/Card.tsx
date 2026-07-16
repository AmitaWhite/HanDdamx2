import type { HTMLAttributes } from "react";
import { cn } from "@/lib/cn";

interface CardProps extends HTMLAttributes<HTMLDivElement> {
	/** hover 시 살짝 떠오르는 상호작용 카드 여부 */
	interactive?: boolean;
}

/** DESIGN.md Elevation Level 1 카드 (흰 배경 + 부드러운 그림자 + 1rem radius) */
export function Card({ interactive, className, ...props }: CardProps) {
	return (
		<div
			className={cn(
				"rounded-xl bg-surface-container-lowest shadow-card",
				interactive && "transition-shadow hover:shadow-card-hover",
				className,
			)}
			{...props}
		/>
	);
}
