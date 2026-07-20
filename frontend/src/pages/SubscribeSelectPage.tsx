import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { cn } from "@/lib/cn";
import { findCreator } from "@/mocks/creators";
import { mockPlans } from "@/mocks/subscriptionPlans";

export function SubscribeSelectPage() {
	const { creatorId = "" } = useParams();
	const creator = findCreator(creatorId);
	const navigate = useNavigate();
	const [selected, setSelected] = useState<"free" | "paid">("paid");

	const selectedPlan = mockPlans.find((p) => p.id === selected) ?? mockPlans[0];

	function handleSubscribe() {
		navigate(paths.subscribeComplete, { state: { creatorId: creator.id, planId: selectedPlan.id } });
	}

	return (
		<div className="container-page max-w-xl py-10">
			<Link
				to={paths.creator(creator.id)}
				className="mb-6 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				{creator.name} 프로필로
			</Link>

			<h1 className="mb-2 text-headline-lg font-display text-on-surface">{creator.name} 작가 구독하기</h1>
			<p className="mb-8 text-body-md text-secondary">원하는 구독 플랜을 선택해주세요.</p>

			<div className="mb-8 flex flex-col gap-4">
				{mockPlans.map((plan) => (
					<button
						key={plan.id}
						type="button"
						onClick={() => setSelected(plan.id)}
						className={cn(
							"relative rounded-xl border p-6 text-left transition-colors",
							selected === plan.id ? "border-primary bg-primary/5" : "border-outline-variant",
						)}
					>
						{plan.isRecommended && (
							<span className="absolute right-6 top-6 rounded-full bg-primary px-3 py-1 text-[10px] font-bold text-on-primary">
								추천
							</span>
						)}
						<div className="flex items-start gap-4">
							<span
								className={cn(
									"mt-1 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2",
									selected === plan.id ? "border-primary" : "border-outline-variant",
								)}
							>
								{selected === plan.id && <span className="h-2.5 w-2.5 rounded-full bg-primary" />}
							</span>
							<div className="flex-1">
								<h2 className="text-headline-md font-display text-on-surface">{plan.name}</h2>
								{plan.description && <p className="mt-1 text-body-md text-secondary">{plan.description}</p>}
								<ul className="mt-3 flex flex-col gap-1.5">
									{plan.benefits.map((b) => (
										<li key={b} className="flex items-center gap-2 text-body-md text-on-surface">
											<Icon name="check" className="text-[18px] text-primary" />
											{b}
										</li>
									))}
								</ul>
								<p className="mt-4 text-headline-md font-display text-on-surface">{plan.priceLabel}</p>
							</div>
						</div>
					</button>
				))}
			</div>

			<Button size="lg" fullWidth onClick={handleSubscribe}>
				결제하고 구독하기
			</Button>
			<p className="mt-4 text-center text-caption font-caption text-secondary">구독은 언제든지 취소할 수 있습니다.</p>
		</div>
	);
}
