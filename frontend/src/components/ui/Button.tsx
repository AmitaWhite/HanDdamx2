import type { ButtonHTMLAttributes } from "react";
import { forwardRef } from "react";
import { cn } from "@/lib/cn";

export type ButtonVariant = "primary" | "secondary" | "outline" | "ghost";
export type ButtonSize = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
	variant?: ButtonVariant;
	size?: ButtonSize;
	fullWidth?: boolean;
}

const base =
	"inline-flex items-center justify-center gap-2 rounded font-label-md font-bold transition-all " +
	"focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/40 " +
	"disabled:opacity-50 disabled:pointer-events-none active:scale-[0.98]";

const variants: Record<ButtonVariant, string> = {
	// #C62839 → hover #A30424
	primary: "bg-primary text-on-primary hover:bg-primary-hover shadow-sm",
	secondary:
		"bg-surface-container-lowest border border-outline-variant text-on-surface hover:bg-surface-container-low",
	outline:
		"border border-primary text-primary hover:bg-primary hover:text-on-primary",
	ghost: "text-on-surface hover:bg-surface-container-low",
};

const sizes: Record<ButtonSize, string> = {
	sm: "h-9 px-4 text-label-md",
	md: "h-11 px-5 text-body-md",
	lg: "h-14 px-8 text-body-md",
};

interface ButtonClassNameOptions {
	variant?: ButtonVariant;
	size?: ButtonSize;
	fullWidth?: boolean;
	className?: string;
}

/** Button과 동일한 스타일 클래스를 계산. LinkButton 등 button 이외 요소에 재사용. */
export function buttonClassName({
	variant = "primary",
	size = "md",
	fullWidth,
	className,
}: ButtonClassNameOptions) {
	return cn(
		base,
		variants[variant],
		sizes[size],
		fullWidth && "w-full",
		className,
	);
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
	(
		{
			variant = "primary",
			size = "md",
			fullWidth,
			type = "button",
			className,
			...props
		},
		ref,
	) => (
		<button
			ref={ref}
			type={type}
			className={buttonClassName({ variant, size, fullWidth, className })}
			{...props}
		/>
	),
);
Button.displayName = "Button";
