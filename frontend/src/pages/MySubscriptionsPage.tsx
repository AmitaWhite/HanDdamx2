import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Alert } from "@/components/ui/Alert";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import {
	cancelFreeSubscription,
	getMySubscriptions,
	revokeSubscriptionCancellation,
	scheduleSubscriptionCancellation,
	type MySubscriptionResponse,
} from "@/features/subscription/subscriptionApi";
import { asApiError } from "@/lib/api";

type SubscriptionAction = "cancel-free" | "schedule-cancel" | "revoke-cancel";

interface ActionDialogState {
	type: SubscriptionAction;
	subscription: MySubscriptionResponse;
}

interface Feedback {
	type: "success" | "error";
	message: string;
}

const dateFormatter = new Intl.DateTimeFormat("ko-KR", {
	year: "numeric",
	month: "numeric",
	day: "numeric",
});

const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
	year: "numeric",
	month: "numeric",
	day: "numeric",
	hour: "2-digit",
	minute: "2-digit",
	hour12: false,
});

export function MySubscriptionsPage() {
	const [subscriptions, setSubscriptions] = useState<MySubscriptionResponse[]>([]);
	const [loading, setLoading] = useState(true);
	const [refreshing, setRefreshing] = useState(false);
	const [loadError, setLoadError] = useState<string | null>(null);
	const [feedback, setFeedback] = useState<Feedback | null>(null);
	const [dialogState, setDialogState] = useState<ActionDialogState | null>(null);
	const [pendingSubscriptionId, setPendingSubscriptionId] = useState<
		number | null
	>(null);

	const mountedRef = useRef(false);
	const loadRequestIdRef = useRef(0);

	const loadSubscriptions = useCallback(
		async ({ initial = false }: { initial?: boolean } = {}) => {
			const requestId = loadRequestIdRef.current + 1;
			loadRequestIdRef.current = requestId;

			if (initial) {
				setLoading(true);
			} else {
				setRefreshing(true);
			}
			setLoadError(null);

			try {
				const data = await getMySubscriptions();
				if (!mountedRef.current || loadRequestIdRef.current !== requestId) {
					return false;
				}
				setSubscriptions(data);
				return true;
			} catch (err) {
				if (!mountedRef.current || loadRequestIdRef.current !== requestId) {
					return false;
				}
				setLoadError(toErrorMessage(err, "구독 목록을 불러오지 못했습니다."));
				return false;
			} finally {
				if (mountedRef.current && loadRequestIdRef.current === requestId) {
					setLoading(false);
					setRefreshing(false);
				}
			}
		},
		[],
	);

	useEffect(() => {
		mountedRef.current = true;
		void loadSubscriptions({ initial: true });

		return () => {
			mountedRef.current = false;
			loadRequestIdRef.current += 1;
		};
	}, [loadSubscriptions]);

	async function handleConfirmAction() {
		if (!dialogState || pendingSubscriptionId !== null) return;

		const { type, subscription } = dialogState;
		setPendingSubscriptionId(subscription.subscriptionId);
		setFeedback(null);

		try {
			if (type === "cancel-free") {
				await cancelFreeSubscription(subscription.creatorId);
			} else if (type === "schedule-cancel") {
				await scheduleSubscriptionCancellation(subscription.subscriptionId);
			} else {
				await revokeSubscriptionCancellation(subscription.subscriptionId);
			}

			if (!mountedRef.current) return;
			setDialogState(null);

			const reloaded = await loadSubscriptions();
			if (!mountedRef.current) return;

			setFeedback({
				type: reloaded ? "success" : "error",
				message: reloaded
					? getActionSuccessMessage(type)
					: `${getActionSuccessMessage(type)} 다만 목록을 새로고침하지 못했습니다. 다시 시도해 주세요.`,
			});
		} catch (err) {
			if (!mountedRef.current) return;
			setFeedback({
				type: "error",
				message: toErrorMessage(err, getActionFallbackMessage(type)),
			});
		} finally {
			if (mountedRef.current) {
				setPendingSubscriptionId(null);
			}
		}
	}

	const showInitialError = !loading && subscriptions.length === 0 && loadError;
	const showEmpty = !loading && !loadError && subscriptions.length === 0;

	return (
		<MyPageShell>
			<div>
				<div className="mb-8">
					<h1 className="text-headline-lg font-display text-on-surface">
						구독 내역
					</h1>
					<p className="mt-1 text-body-md text-secondary">
						구독 중인 크리에이터와 이용 상태를 확인하고 관리합니다.
					</p>
				</div>

				{feedback?.type === "success" && (
					<p
						role="status"
						className="mb-4 rounded border border-primary/30 bg-primary/5 px-4 py-3 text-label-md font-label-md text-primary"
					>
						{feedback.message}
					</p>
				)}

				{feedback?.type === "error" && (
					<div className="mb-4">
						<Alert>{feedback.message}</Alert>
					</div>
				)}

				{loadError && subscriptions.length > 0 && (
					<div className="mb-4 flex flex-col gap-3 rounded border border-outline-variant/60 bg-surface-container-lowest p-4 sm:flex-row sm:items-center sm:justify-between">
						<p className="text-body-md text-secondary">{loadError}</p>
						<Button
							type="button"
							variant="outline"
							size="sm"
							disabled={refreshing}
							onClick={() => {
								void loadSubscriptions();
							}}
						>
							{refreshing ? "다시 불러오는 중" : "다시 시도"}
						</Button>
					</div>
				)}

				{refreshing && subscriptions.length > 0 && (
					<p role="status" className="mb-4 text-body-sm text-secondary">
						구독 목록을 새로고침 중입니다.
					</p>
				)}

				{loading && (
					<p role="status" className="py-12 text-center text-body-md text-secondary">
						구독 목록을 불러오는 중입니다.
					</p>
				)}

				{showInitialError && (
					<Card className="p-6 text-center">
						<Alert>{loadError}</Alert>
						<Button
							type="button"
							variant="outline"
							className="mt-5"
							onClick={() => {
								void loadSubscriptions({ initial: true });
							}}
						>
							다시 시도
						</Button>
					</Card>
				)}

				{showEmpty && (
					<div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-outline-variant py-12 text-center">
						<Icon name="subscriptions" className="text-[32px] text-secondary" />
						<p className="text-body-md text-on-surface">
							현재 구독 중인 크리에이터가 없습니다.
						</p>
						<p className="text-caption font-caption text-secondary">
							관심 있는 크리에이터를 찾아 무료 또는 유료 구독을 시작해 보세요.
						</p>
					</div>
				)}

				{!loading && subscriptions.length > 0 && (
					<div className="flex flex-col gap-4">
						{subscriptions.map((subscription) => (
							<SubscriptionCard
								key={subscription.subscriptionId}
								subscription={subscription}
								pending={pendingSubscriptionId === subscription.subscriptionId}
								onActionClick={(type) => {
									setFeedback(null);
									setDialogState({ type, subscription });
								}}
							/>
						))}
					</div>
				)}
			</div>

			<ConfirmDialog
				open={dialogState !== null}
				title={dialogState ? getDialogTitle(dialogState.type) : ""}
				description={
					dialogState
						? getDialogDescription(dialogState.type, dialogState.subscription)
						: undefined
				}
				confirmLabel={
					pendingSubscriptionId !== null
						? "처리 중"
						: dialogState
							? getActionLabel(dialogState.type)
							: "확인"
				}
				cancelLabel="닫기"
				onCancel={() => {
					if (pendingSubscriptionId === null) setDialogState(null);
				}}
				onConfirm={() => {
					void handleConfirmAction();
				}}
			/>
		</MyPageShell>
	);
}

function SubscriptionCard({
	subscription,
	pending,
	onActionClick,
}: {
	subscription: MySubscriptionResponse;
	pending: boolean;
	onActionClick: (type: SubscriptionAction) => void;
}) {
	const action = getAvailableAction(subscription);
	const statusMeta = getStatusMeta(subscription);

	return (
		<Card className="p-5 md:p-6">
			<div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
				<div className="min-w-0 flex-1">
					<Link
						to={paths.creator(subscription.creatorId)}
						className="flex min-w-0 items-center gap-4 rounded-lg transition-opacity hover:opacity-80"
					>
						<Avatar
							src={subscription.creatorProfileImageUrl ?? undefined}
							alt={subscription.creatorNickname}
							fallbackText={subscription.creatorNickname}
							size={56}
						/>
						<div className="min-w-0">
							<p className="truncate text-title-md font-title-md text-on-surface">
								{subscription.creatorNickname}
							</p>
							<p className="mt-1 text-body-sm text-secondary">
								크리에이터 페이지로 이동
							</p>
						</div>
					</Link>

					<div className="mt-5 flex flex-wrap items-center gap-2">
						<Chip size="sm" active={subscription.subscriptionLevel === "PAID"}>
							{getSubscriptionTypeLabel(subscription)}
						</Chip>
						<Chip size="sm" className={statusMeta.className}>
							{statusMeta.label}
						</Chip>
					</div>

					<div className="mt-5 grid gap-3 sm:grid-cols-2">
						<InfoItem
							label="구독 시작일"
							value={formatDate(subscription.startedAt)}
						/>
						<InfoItem
							label={getPeriodLabel(subscription)}
							value={getPeriodValue(subscription)}
						/>
						{isPaidCancelScheduled(subscription) &&
							subscription.cancelScheduledAt && (
								<InfoItem
									label="해지 예약 일시"
									value={formatDateTime(subscription.cancelScheduledAt)}
								/>
							)}
					</div>
				</div>

				<div className="flex flex-col gap-2 sm:flex-row lg:w-40 lg:flex-col">
					<LinkButton
						to={paths.creator(subscription.creatorId)}
						variant="secondary"
						size="sm"
						fullWidth
					>
						크리에이터 페이지
					</LinkButton>
					{action && (
						<Button
							type="button"
							variant="outline"
							size="sm"
							fullWidth
							disabled={pending}
							onClick={() => onActionClick(action)}
						>
							{pending ? "처리 중" : getActionLabel(action)}
						</Button>
					)}
				</div>
			</div>
		</Card>
	);
}

function InfoItem({ label, value }: { label: string; value: string }) {
	return (
		<div className="rounded border border-outline-variant/50 bg-surface-container-low px-4 py-3">
			<p className="text-caption font-caption text-secondary">{label}</p>
			<p className="mt-1 text-body-md font-bold text-on-surface">{value}</p>
		</div>
	);
}

function getAvailableAction(
	subscription: MySubscriptionResponse,
): SubscriptionAction | null {
	if (isFreeActive(subscription)) return "cancel-free";
	if (isPaidActive(subscription)) return "schedule-cancel";
	if (isPaidCancelScheduled(subscription)) return "revoke-cancel";
	return null;
}

function getSubscriptionTypeLabel(subscription: MySubscriptionResponse): string {
	if (subscription.subscriptionLevel === "FREE") return "무료";
	if (subscription.subscriptionLevel === "PAID") return "유료 구독";
	return subscription.subscriptionLevel;
}

function getStatusMeta(subscription: MySubscriptionResponse): {
	label: string;
	className: string;
} {
	if (isFreeActive(subscription)) {
		return {
			label: "무료 구독 중",
			className: "bg-surface-container text-secondary",
		};
	}
	if (isPaidActive(subscription)) {
		return {
			label: "유료 구독 중",
			className: "bg-primary text-on-primary",
		};
	}
	if (isPaidCancelScheduled(subscription)) {
		return {
			label: "해지 예약",
			className: "bg-tertiary text-on-tertiary",
		};
	}
	return {
		label: "상태 확인 필요",
		className: "bg-error-container text-on-error-container",
	};
}

function getPeriodLabel(subscription: MySubscriptionResponse): string {
	if (isPaidCancelScheduled(subscription)) return "혜택 이용 가능";
	if (subscription.subscriptionLevel === "PAID") return "현재 이용 종료일";
	return "이용 종료일";
}

function getPeriodValue(subscription: MySubscriptionResponse): string {
	if (isPaidCancelScheduled(subscription)) {
		return subscription.currentPeriodEndAt
			? `${formatDate(subscription.currentPeriodEndAt)}까지`
			: "종료일 확인 필요";
	}

	if (subscription.subscriptionLevel === "PAID") {
		return subscription.currentPeriodEndAt
			? formatDate(subscription.currentPeriodEndAt)
			: "종료일 확인 필요";
	}

	if (subscription.subscriptionLevel === "FREE") {
		return "별도 종료일 없음";
	}

	return "상태 확인 필요";
}

function isFreeActive(subscription: MySubscriptionResponse): boolean {
	return (
		subscription.subscriptionLevel === "FREE" && subscription.status === "ACTIVE"
	);
}

function isPaidActive(subscription: MySubscriptionResponse): boolean {
	return (
		subscription.subscriptionLevel === "PAID" && subscription.status === "ACTIVE"
	);
}

function isPaidCancelScheduled(subscription: MySubscriptionResponse): boolean {
	return (
		subscription.subscriptionLevel === "PAID" &&
		subscription.status === "CANCEL_SCHEDULED"
	);
}

function getDialogTitle(action: SubscriptionAction): string {
	switch (action) {
		case "cancel-free":
			return "무료 구독을 취소하시겠어요?";
		case "schedule-cancel":
			return "유료 구독 해지를 예약하시겠어요?";
		case "revoke-cancel":
			return "해지 예약을 철회하시겠어요?";
	}
}

function getDialogDescription(
	action: SubscriptionAction,
	subscription: MySubscriptionResponse,
): string {
	switch (action) {
		case "cancel-free":
			return "취소하면 즉시 해당 크리에이터의 무료 구독 혜택을 이용할 수 없습니다.";
		case "schedule-cancel": {
			const endAt = subscription.currentPeriodEndAt
				? formatDate(subscription.currentPeriodEndAt)
				: null;
			return endAt
				? `해지를 예약해도 현재 이용 기간 종료일인 ${endAt}까지 혜택을 이용할 수 있습니다.`
				: "해지를 예약해도 현재 이용 기간 종료일까지 혜택을 이용할 수 있습니다.";
		}
		case "revoke-cancel":
			return "해지 예약을 철회하면 구독 상태가 다시 활성으로 돌아갑니다.";
	}
}

function getActionLabel(action: SubscriptionAction): string {
	switch (action) {
		case "cancel-free":
			return "무료 구독 취소";
		case "schedule-cancel":
			return "해지 예약";
		case "revoke-cancel":
			return "해지 예약 철회";
	}
}

function getActionSuccessMessage(action: SubscriptionAction): string {
	switch (action) {
		case "cancel-free":
			return "무료 구독을 취소했습니다.";
		case "schedule-cancel":
			return "유료 구독 해지를 예약했습니다.";
		case "revoke-cancel":
			return "해지 예약을 철회했습니다.";
	}
}

function getActionFallbackMessage(action: SubscriptionAction): string {
	switch (action) {
		case "cancel-free":
			return "무료 구독을 취소하지 못했습니다.";
		case "schedule-cancel":
			return "유료 구독 해지를 예약하지 못했습니다.";
		case "revoke-cancel":
			return "해지 예약을 철회하지 못했습니다.";
	}
}

function formatDate(value: string | null): string {
	if (!value) return "날짜 확인 필요";
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return value;
	return dateFormatter.format(date);
}

function formatDateTime(value: string | null): string {
	if (!value) return "날짜 확인 필요";
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return value;
	return dateTimeFormatter.format(date);
}

function toErrorMessage(err: unknown, fallback: string): string {
	const apiError = asApiError(err, fallback);
	return apiError.message || fallback;
}
