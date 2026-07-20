import { useLocation } from "react-router-dom";
import { paths } from "@/app/paths";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { findCreator } from "@/mocks/creators";
import { mockPlans } from "@/mocks/subscriptionPlans";

interface LocationState {
	creatorId?: string;
	planId?: "free" | "paid";
}

export function SubscribeCompletePage() {
	const location = useLocation();
	const state = (location.state ?? {}) as LocationState;
	const creator = findCreator(state.creatorId ?? "suyeon");
	const plan = mockPlans.find((p) => p.id === (state.planId ?? "paid")) ?? mockPlans[1];

	return (
		<div className="container-page flex max-w-xl flex-col items-center py-16 text-center">
			<div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-primary/10 text-primary">
				<Icon name="check_circle" className="text-[32px]" />
			</div>
			<h1 className="mb-2 text-headline-lg font-display text-on-surface">구독 결제가 완료되었어요</h1>
			<p className="mb-8 text-body-md text-secondary">
				{creator.name} 작가의 {plan.name}을 구독했습니다.
			</p>

			<Card className="mb-8 w-full divide-y divide-outline-variant/50 p-6 text-left">
				<Row label="크리에이터" value={creator.name} />
				<Row label="플랜" value={plan.name} />
				<Row label="결제 금액" value={plan.price === 0 ? "무료" : `${plan.price.toLocaleString()}원`} />
				<Row label="결제 일시" value="2026.07.09 14:22" />
			</Card>

			<div className="flex w-full gap-3">
				<LinkButton to={paths.mypage} variant="outline" fullWidth>
					결제 내역 보기
				</LinkButton>
				<LinkButton to={paths.creator(creator.id)} fullWidth>
					작가 페이지로 이동
				</LinkButton>
			</div>
		</div>
	);
}

function Row({ label, value }: { label: string; value: string }) {
	return (
		<div className="flex items-center justify-between py-3">
			<span className="text-body-md text-secondary">{label}</span>
			<span className="text-body-md font-bold text-on-surface">{value}</span>
		</div>
	);
}
