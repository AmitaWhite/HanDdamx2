import type { ReactNode } from "react";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";

type AuthStatusPanelProps =
	| {
			variant: "loading";
			description: ReactNode;
			withCard?: boolean;
			className?: string;
	  }
	| {
			variant: "primary" | "error";
			icon: string;
			title: string;
			description: ReactNode;
			children?: ReactNode;
			withCard?: boolean;
			className?: string;
	  };

/**
 * 인증 흐름의 "처리 중 / 완료 / 실패" 상태를 보여주는 패널.
 * success 는 이 디자인시스템에 별도 색이 없어 primary 톤을 그대로 사용한다.
 */
export function AuthStatusPanel(props: AuthStatusPanelProps) {
	const { withCard, className } = props;

	const content =
		props.variant === "loading" ? (
			<div className="flex flex-col items-center gap-4">
				<Icon
					name="progress_activity"
					className="animate-spin text-[32px] text-primary"
				/>
				<p className="text-body-md text-secondary">{props.description}</p>
			</div>
		) : (
			<>
				<div
					className={cn(
						"mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-full",
						props.variant === "primary"
							? "bg-primary/10 text-primary"
							: "bg-error-container text-on-error-container",
					)}
				>
					<Icon name={props.icon} className="text-[32px]" />
				</div>
				<h1 className="mb-2 text-headline-md font-display text-on-surface">
					{props.title}
				</h1>
				<p className="mb-6 text-body-md text-secondary">{props.description}</p>
				{props.children}
			</>
		);

	if (!withCard) return <div className={className}>{content}</div>;
	return (
		<Card
			className={cn("w-full max-w-[440px] p-8 text-center md:p-10", className)}
		>
			{content}
		</Card>
	);
}
