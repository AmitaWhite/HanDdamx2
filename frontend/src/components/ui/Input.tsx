import type { InputHTMLAttributes } from "react";
import { forwardRef, useId } from "react";
import { cn } from "@/lib/cn";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
	label?: string;
	error?: string;
}

/** 라벨이 위에 오는 표준 입력 필드 (DESIGN.md Input 규칙). */
export const Input = forwardRef<HTMLInputElement, InputProps>(
	({ label, error, className, id, ...props }, ref) => {
		const generatedId = useId();
		const inputId = id ?? generatedId;
		return (
			<div className="flex flex-col gap-1.5">
				{label && (
					<label
						htmlFor={inputId}
						className="text-label-md font-label-md text-on-surface"
					>
						{label}
					</label>
				)}
				<input
					ref={ref}
					id={inputId}
					className={cn(
						"h-11 w-full rounded border bg-surface-container-lowest px-4 text-body-md",
						"placeholder:text-outline focus:outline-none focus:ring-1",
						error
							? "border-error focus:border-error focus:ring-error"
							: "border-outline-variant focus:border-on-surface focus:ring-on-surface",
						className,
					)}
					aria-invalid={error ? true : undefined}
					{...props}
				/>
				{error && (
					<p className="text-caption font-caption text-error">{error}</p>
				)}
			</div>
		);
	},
);
Input.displayName = "Input";
