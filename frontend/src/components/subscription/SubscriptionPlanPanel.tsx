import {
	type ReactNode,
	useCallback,
	useEffect,
	useMemo,
	useRef,
	useState,
} from "react";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import {
	cancelFreeSubscription,
	createFreeSubscription,
	getSubscriptionPlans,
	getSubscriptionStatus,
	type SubscriptionPlanResponse,
	type SubscriptionStatusResponse,
} from "@/features/subscription/subscriptionApi";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";

interface SubscriptionPlanPanelProps {
	creatorId: number;
	creatorNickname: string;
	isOwnCreator: boolean;
}

type ActionState = "create-free" | "cancel-free" | null;

const currencyFormatter = new Intl.NumberFormat("ko-KR");
const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
	year: "numeric",
	month: "long",
	day: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

export function SubscriptionPlanPanel({
	creatorId,
	creatorNickname,
	isOwnCreator,
}: SubscriptionPlanPanelProps) {
	const { isAuthenticated } = useAuth();
	const [status, setStatus] = useState<SubscriptionStatusResponse | null>(null);
	const [plans, setPlans] = useState<SubscriptionPlanResponse[]>([]);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [notice, setNotice] = useState<string | null>(null);
	const [action, setAction] = useState<ActionState>(null);
	const [cancelDialogOpen, setCancelDialogOpen] = useState(false);
	const requestIdRef = useRef(0);
	const mountedRef = useRef(false);

	const freePlan = useMemo(
		() => plans.find((plan) => plan.subscriptionLevel === "FREE") ?? null,
		[plans],
	);
	const paidPlan = useMemo(
		() => plans.find((plan) => plan.subscriptionLevel === "PAID") ?? null,
		[plans],
	);

	const isUnsubscribed =
		status?.subscribed === false &&
		status.subscriptionLevel === null &&
		status.status === null;
	const isFreeActive =
		status?.subscribed === true &&
		status.subscriptionLevel === "FREE" &&
		status.status === "ACTIVE";
	const isPaidActive =
		status?.subscribed === true &&
		status.subscriptionLevel === "PAID" &&
		status.status === "ACTIVE";
	const isPaidCancelScheduled =
		status?.subscribed === true &&
		status.subscriptionLevel === "PAID" &&
		status.status === "CANCEL_SCHEDULED";
	const isUnexpectedStatus =
		status !== null &&
		!isUnsubscribed &&
		!isFreeActive &&
		!isPaidActive &&
		!isPaidCancelScheduled;
	const actionRunning = action !== null;

	const loadSubscriptionData = useCallback(async () => {
		const requestId = requestIdRef.current + 1;
		requestIdRef.current = requestId;
		setLoading(true);
		setError(null);
		setNotice(null);

		try {
			const [nextStatus, nextPlans] = await Promise.all([
				getSubscriptionStatus(creatorId),
				getSubscriptionPlans(creatorId),
			]);

			if (!mountedRef.current || requestIdRef.current !== requestId) return;
			setStatus(nextStatus);
			setPlans(nextPlans);
		} catch (err) {
			if (!mountedRef.current || requestIdRef.current !== requestId) return;
			setStatus(null);
			setPlans([]);
			setError(toUserMessage(err, "구독 정보를 불러오지 못했습니다."));
		} finally {
			if (mountedRef.current && requestIdRef.current === requestId) {
				setLoading(false);
			}
		}
	}, [creatorId]);

	const reloadStatus = useCallback(async (): Promise<boolean> => {
		const requestId = requestIdRef.current;
		const nextStatus = await getSubscriptionStatus(creatorId);
		if (!mountedRef.current || requestIdRef.current !== requestId) return false;
		setStatus(nextStatus);
		return true;
	}, [creatorId]);

	useEffect(() => {
		mountedRef.current = true;
		return () => {
			mountedRef.current = false;
			requestIdRef.current += 1;
		};
	}, []);

	useEffect(() => {
		requestIdRef.current += 1;
		setStatus(null);
		setPlans([]);
		setError(null);
		setNotice(null);
		setAction(null);
		setCancelDialogOpen(false);

		if (!isAuthenticated || isOwnCreator) {
			setLoading(false);
			return;
		}

		void loadSubscriptionData();
	}, [isAuthenticated, isOwnCreator, loadSubscriptionData]);

	async function handleCreateFreeSubscription() {
		if (actionRunning || !isUnsubscribed) return;

		const requestId = requestIdRef.current;
		setAction("create-free");
		setError(null);
		setNotice(null);
		try {
			await createFreeSubscription(creatorId);
			const reloaded = await reloadStatus();
			if (reloaded) {
				setNotice("무료 구독을 시작했습니다.");
			}
		} catch (err) {
			if (mountedRef.current && requestIdRef.current === requestId) {
				setError(toUserMessage(err, "무료 구독을 시작하지 못했습니다."));
			}
		} finally {
			if (mountedRef.current && requestIdRef.current === requestId) {
				setAction(null);
			}
		}
	}

	async function handleConfirmCancelFreeSubscription() {
		if (actionRunning || !isFreeActive) return;

		const requestId = requestIdRef.current;
		setAction("cancel-free");
		setError(null);
		setNotice(null);
		try {
			await cancelFreeSubscription(creatorId);
			const reloaded = await reloadStatus();
			if (reloaded) {
				setNotice("무료 구독을 취소했습니다.");
				setCancelDialogOpen(false);
			}
		} catch (err) {
			if (mountedRef.current && requestIdRef.current === requestId) {
				setError(toUserMessage(err, "무료 구독을 취소하지 못했습니다."));
			}
		} finally {
			if (mountedRef.current && requestIdRef.current === requestId) {
				setAction(null);
			}
		}
	}

	if (isOwnCreator) {
		return (
			<Card className="p-6">
				<h2 className="text-headline-md font-display text-on-surface">
					구독
				</h2>
				<p className="mt-2 text-body-md text-secondary">
					본인 크리에이터 페이지에서는 구독을 신청할 수 없습니다.
				</p>
			</Card>
		);
	}

	if (!isAuthenticated) {
		return (
			<Card className="p-6">
				<h2 className="text-headline-md font-display text-on-surface">
					구독
				</h2>
				<p className="mt-2 text-body-md text-secondary">
					{creatorNickname}님의 구독 요금제를 확인하고 구독하려면 로그인이 필요합니다.
				</p>
				<div className="mt-5">
					<LinkButton to={paths.login}>로그인하러 가기</LinkButton>
				</div>
			</Card>
		);
	}

	return (
		<Card className="p-6">
			<div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
				<div>
					<h2 className="text-headline-md font-display text-on-surface">
						구독
					</h2>
					<p className="mt-1 text-body-md text-secondary">
						{creatorNickname}님의 무료 및 유료 구독 상태를 확인합니다.
					</p>
				</div>
				{status && <StatusBadge label={getStatusLabel(status)} />}
			</div>

			{loading && (
				<p
					role="status"
					className="mt-5 rounded border border-outline-variant/50 bg-surface-container-low p-4 text-body-md text-secondary"
				>
					구독 정보를 불러오는 중입니다.
				</p>
			)}

			{error && (
				<div className="mt-5">
					<Alert>{error}</Alert>
					<Button
						type="button"
						variant="outline"
						className="mt-3"
						disabled={loading}
						onClick={() => {
							void loadSubscriptionData();
						}}
					>
						다시 시도
					</Button>
				</div>
			)}

			{notice && !error && (
				<p
					role="status"
					className="mt-5 rounded border border-primary/30 bg-primary/5 px-4 py-3 text-label-md font-label-md text-primary"
				>
					{notice}
				</p>
			)}

			{!loading && !error && plans.length === 0 && (
				<p className="mt-5 rounded border border-outline-variant/50 bg-surface-container-low p-4 text-body-md text-secondary">
					사용 가능한 구독 요금제가 없습니다.
				</p>
			)}

			{!loading && !error && plans.length > 0 && (
				<div className="mt-6 grid gap-4 md:grid-cols-2">
					{freePlan && (
						<PlanCard
							plan={freePlan}
							title="무료 구독"
							description={
								freePlan.benefitsDescription ??
								"크리에이터의 공개 소식을 구독합니다."
							}
						>
							{isUnsubscribed && (
								<Button
									type="button"
									fullWidth
									disabled={actionRunning}
									onClick={() => {
										void handleCreateFreeSubscription();
									}}
								>
									{action === "create-free"
										? "무료 구독 처리 중"
										: "무료 구독"}
								</Button>
							)}
							{isFreeActive && (
								<Button
									type="button"
									variant="outline"
									fullWidth
									disabled={actionRunning}
									onClick={() => setCancelDialogOpen(true)}
								>
									{action === "cancel-free"
										? "무료 구독 취소 중"
										: "무료 구독 취소"}
								</Button>
							)}
							{(isPaidActive || isPaidCancelScheduled) && (
								<p className="text-label-md font-label-md text-secondary">
									유료 구독 이용 중에는 무료 구독을 변경할 수 없습니다.
								</p>
							)}
						</PlanCard>
					)}

					{paidPlan && (
						<PlanCard
							plan={paidPlan}
							title="유료 구독"
							description={paidPlan.benefitsDescription}
						>
							{(isUnsubscribed || isFreeActive) && (
								<LinkButton
									to={paths.subscribeSelect(creatorId)}
									state={isFreeActive ? { mode: "support" } : undefined}
									fullWidth
									variant={paidPlan.available ? "primary" : "secondary"}
									aria-disabled={!paidPlan.available || actionRunning}
									className={cn(
										(!paidPlan.available || actionRunning) &&
											"pointer-events-none opacity-50",
									)}
								>
									{paidPlan.available
										? "유료 구독 선택"
										: "유료 구독 준비 중"}
								</LinkButton>
							)}
							{isPaidActive && (
								<p className="text-label-md font-label-md text-primary">
									현재 이용 중인 요금제입니다.
								</p>
							)}
							{isPaidCancelScheduled && (
								<p className="text-label-md font-label-md text-primary">
									해지 예약된 유료 구독입니다. 마이페이지에서 관리할 수 있습니다.
								</p>
							)}
						</PlanCard>
					)}
				</div>
			)}

			{!loading && !error && isPaidCancelScheduled && status?.currentPeriodEndAt && (
				<p className="mt-4 rounded border border-outline-variant/50 bg-surface-container-low px-4 py-3 text-body-md text-secondary">
					혜택 종료 예정일: {formatDateTime(status.currentPeriodEndAt)}
				</p>
			)}

			{!loading && !error && isUnexpectedStatus && (
				<p className="mt-4 rounded border border-outline-variant/50 bg-surface-container-low px-4 py-3 text-body-md text-secondary">
					현재 구독 상태를 확인 중입니다. 문제가 계속되면 잠시 후 다시 시도해 주세요.
				</p>
			)}

			<ConfirmDialog
				open={cancelDialogOpen}
				title="무료 구독을 취소할까요?"
				description="취소하면 이 크리에이터의 무료 구독 상태가 해제됩니다."
				confirmLabel={
					action === "cancel-free" ? "취소 처리 중" : "무료 구독 취소"
				}
				cancelLabel="닫기"
				onCancel={() => {
					if (!actionRunning) setCancelDialogOpen(false);
				}}
				onConfirm={() => {
					void handleConfirmCancelFreeSubscription();
				}}
			/>
		</Card>
	);
}

function PlanCard({
	plan,
	title,
	description,
	children,
}: {
	plan: SubscriptionPlanResponse;
	title: string;
	description: string | null;
	children: ReactNode;
}) {
	return (
		<article
			className={cn(
				"flex min-h-[220px] flex-col rounded border p-5",
				plan.available
					? "border-outline-variant bg-surface-container-lowest"
					: "border-outline-variant/60 bg-surface-container-low",
			)}
		>
			<div className="flex items-center justify-between gap-3">
				<h3 className="text-title-md font-title-md text-on-surface">
					{title}
				</h3>
				<span className="rounded bg-surface-container px-2 py-1 text-caption font-caption text-secondary">
					{plan.subscriptionLevel}
				</span>
			</div>
			<p className="mt-3 text-headline-md font-display text-on-surface">
				{formatPrice(plan)}
			</p>
			{description && (
				<p className="mt-3 flex-1 text-body-md text-secondary">
					{description}
				</p>
			)}
			{!plan.available && (
				<p className="mt-3 flex-1 text-body-md text-secondary">
					현재 선택할 수 없는 요금제입니다.
				</p>
			)}
			<div className="mt-5">{children}</div>
		</article>
	);
}

function StatusBadge({ label }: { label: string }) {
	return (
		<span className="inline-flex w-fit items-center rounded bg-primary px-3 py-1 text-caption font-caption text-on-primary">
			{label}
		</span>
	);
}

function getStatusLabel(status: SubscriptionStatusResponse): string {
	if (
		status.subscribed === false &&
		status.subscriptionLevel === null &&
		status.status === null
	) {
		return "미구독";
	}
	if (status.subscriptionLevel === "FREE" && status.status === "ACTIVE") {
		return "무료 구독 중";
	}
	if (status.subscriptionLevel === "PAID" && status.status === "ACTIVE") {
		return "유료 구독 중";
	}
	if (
		status.subscriptionLevel === "PAID" &&
		status.status === "CANCEL_SCHEDULED"
	) {
		return "해지 예약";
	}
	return "상태 확인 필요";
}

function formatPrice(plan: SubscriptionPlanResponse): string {
	if (plan.subscriptionLevel === "FREE" || plan.price === 0) {
		return "무료";
	}
	return `${currencyFormatter.format(plan.price)}원 / 월`;
}

function formatDateTime(value: string): string {
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) {
		return value;
	}
	return dateTimeFormatter.format(date);
}

function toUserMessage(err: unknown, fallback: string): string {
	if (!(err instanceof ApiError)) {
		return fallback;
	}

	switch (err.code) {
		case "UNAUTHORIZED":
			return "로그인이 만료되었습니다. 다시 로그인해 주세요.";
		case "SELF_SUBSCRIPTION_NOT_ALLOWED":
			return "본인 크리에이터 계정은 구독할 수 없습니다.";
		case "SUBSCRIPTION_ALREADY_EXISTS":
			return "이미 구독 중인 크리에이터입니다.";
		case "FREE_SUBSCRIPTION_NOT_FOUND":
			return "취소할 무료 구독을 찾을 수 없습니다.";
		case "PAID_SUBSCRIPTION_CANNOT_BE_CANCELED_AS_FREE":
			return "유료 구독은 무료 구독 취소로 해지할 수 없습니다.";
		case "LOCK_ACQUISITION_TIMEOUT":
			return "현재 동일한 작업을 처리 중입니다. 잠시 후 다시 시도해 주세요.";
		default:
			return err.message || fallback;
	}
}
