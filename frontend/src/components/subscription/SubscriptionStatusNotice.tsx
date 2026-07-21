import { Card } from "@/components/ui/Card";

interface SubscriptionStatusNoticeProps {
	label: string;
	description: string;
	periodEndAtLabel?: string | null;
}

export function SubscriptionStatusNotice({
	label,
	description,
	periodEndAtLabel,
}: SubscriptionStatusNoticeProps) {
	return (
		<Card className="mb-6 p-6">
			<div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
				<div>
					<h2 className="text-headline-md font-display text-on-surface">
						현재 구독 상태
					</h2>
					<p className="mt-2 text-body-md text-secondary">{description}</p>
				</div>
				<span className="inline-flex w-fit rounded bg-primary px-3 py-1 text-caption font-caption text-on-primary">
					{label}
				</span>
			</div>
			{periodEndAtLabel && (
				<p className="mt-4 rounded border border-outline-variant/50 bg-surface-container-low px-4 py-3 text-body-md text-secondary">
					혜택 종료 예정일: {periodEndAtLabel}
				</p>
			)}
		</Card>
	);
}
