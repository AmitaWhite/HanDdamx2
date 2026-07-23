import type { ReactNode } from "react";
import type { SubscriptionPlanResponse } from "@/features/subscription/subscriptionApi";
import { cn } from "@/lib/cn";

interface SubscriptionPlanCardProps {
	plan: SubscriptionPlanResponse;
	title: string;
	description: string | null;
	priceLabel: string;
	current?: boolean;
	selected?: boolean;
	children: ReactNode;
}

export function SubscriptionPlanCard({
	plan,
	title,
	description,
	priceLabel,
	current = false,
	selected = false,
	children,
}: SubscriptionPlanCardProps) {
	return (
		<article
			className={cn(
				"relative flex min-h-[260px] flex-col rounded-xl border p-6 transition-colors",
				selected ? "border-primary bg-primary/5" : "border-outline-variant",
				!plan.available && "bg-surface-container-low",
			)}
		>
			<div className="flex items-start gap-4">
				<span
					className={cn(
						"mt-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2",
						selected ? "border-primary" : "border-outline-variant",
					)}
					aria-hidden
				>
					{selected && <span className="h-2.5 w-2.5 rounded-full bg-primary" />}
				</span>
				<div className="min-w-0 flex-1">
					<div className="flex flex-wrap items-center gap-2">
						<h2 className="text-headline-md font-display text-on-surface">
							{title}
						</h2>
						<span className="rounded-full bg-surface-container px-3 py-1 text-caption font-caption text-secondary">
							{plan.subscriptionLevel}
						</span>
						{current && (
							<span className="rounded-full bg-primary px-3 py-1 text-caption font-caption text-on-primary">
								현재 이용 중
							</span>
						)}
						{!plan.available && (
							<span className="rounded-full bg-surface-container px-3 py-1 text-caption font-caption text-secondary">
								이용 불가
							</span>
						)}
					</div>
					<p className="mt-4 text-headline-md font-display text-on-surface">
						{priceLabel}
					</p>
					<p className="mt-3 text-body-md text-secondary">
						{description ?? "등록된 상세 혜택 설명이 없습니다."}
					</p>
					{!plan.available && (
						<p className="mt-3 text-body-md text-secondary">
							현재 선택할 수 없는 요금제입니다.
						</p>
					)}
				</div>
			</div>
			<div className="mt-auto pt-6">{children}</div>
		</article>
	);
}
