import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";

interface CheckoutSummaryProps {
	creatorNickname: string;
	amountLabel: string;
	notice: string | null;
	disabled: boolean;
	disabledReason: string;
	onReadyClick: () => void;
}

export function CheckoutSummary({
	creatorNickname,
	amountLabel,
	notice,
	disabled,
	disabledReason,
	onReadyClick,
}: CheckoutSummaryProps) {
	return (
		<Card className="p-6">
			<h2 className="text-headline-md font-display text-on-surface">
				결제 전 확인
			</h2>
			<div className="mt-5 divide-y divide-outline-variant/50">
				<SummaryRow label="크리에이터" value={creatorNickname} />
				<SummaryRow label="선택한 요금제" value="유료 구독" />
				<SummaryRow label="결제 금액" value={amountLabel} />
			</div>
			<p className="mt-5 text-body-md text-secondary">
				다음 단계에서 결제 준비 요청과 Toss Payments 결제창을 한 흐름으로 연결합니다.
			</p>
			{notice && (
				<p
					role="status"
					className="mt-4 rounded border border-primary/30 bg-primary/5 px-4 py-3 text-label-md font-label-md text-primary"
				>
					{notice}
				</p>
			)}
			<Button
				type="button"
				size="lg"
				fullWidth
				className="mt-6"
				disabled={disabled}
				onClick={onReadyClick}
			>
				결제 연동 준비 중
			</Button>
			{disabled && (
				<p className="mt-3 text-center text-caption font-caption text-secondary">
					{disabledReason}
				</p>
			)}
		</Card>
	);
}

function SummaryRow({ label, value }: { label: string; value: string }) {
	return (
		<div className="flex items-center justify-between gap-4 py-3">
			<span className="text-body-md text-secondary">{label}</span>
			<span className="text-right text-body-md font-bold text-on-surface">
				{value}
			</span>
		</div>
	);
}
