import {
	type ReactNode,
	useCallback,
	useEffect,
	useMemo,
	useRef,
	useState,
} from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { CheckoutSummary } from "@/components/subscription/CheckoutSummary";
import { SubscriptionPlanCard } from "@/components/subscription/SubscriptionPlanCard";
import { SubscriptionStatusNotice } from "@/components/subscription/SubscriptionStatusNotice";
import { useAuth } from "@/features/auth/AuthContext";
import { getCreatorProfile } from "@/features/creator/creatorApi";
import type { CreatorProfile } from "@/features/creator/types";
import { preparePayment } from "@/features/payment/paymentApi";
import {
	CheckoutFlowError,
	buildTossRedirectUrls,
	getConfiguredTossClientKey,
	getTossPayments,
	toTossCheckoutErrorInfo,
	validatePreparedPayment,
	type TossCheckoutStage,
} from "@/features/payment/tossPayment";
import {
	getSubscriptionPlans,
	getSubscriptionStatus,
	type SubscriptionPlanResponse,
	type SubscriptionStatusResponse,
} from "@/features/subscription/subscriptionApi";
import { ApiError } from "@/lib/api";

interface SubscribeSelectLocationState {
	mode?: "support";
}

type LoadErrorSource = "creator" | "plans" | "status";
type SelectablePlan = "PAID" | null;

const currencyFormatter = new Intl.NumberFormat("ko-KR");
const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
	year: "numeric",
	month: "long",
	day: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

class SubscribeSelectLoadError extends Error {
	constructor(
		public source: LoadErrorSource,
		public original: unknown,
	) {
		super("Subscribe select data load failed.");
	}
}

export function SubscribeSelectPage() {
	const { creatorId = "" } = useParams();
	const location = useLocation();
	const { isAuthenticated } = useAuth();
	const numericCreatorId = toNumericCreatorId(creatorId);
	const isSupportMode = (location.state as SubscribeSelectLocationState | null)?.mode === "support";

	const [creator, setCreator] = useState<CreatorProfile | null>(null);
	const [plans, setPlans] = useState<SubscriptionPlanResponse[]>([]);
	const [subscriptionStatus, setSubscriptionStatus] =
		useState<SubscriptionStatusResponse | null>(null);
	const [selectedPlan, setSelectedPlan] = useState<SelectablePlan>(null);
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [notice, setNotice] = useState<string | null>(null);
	const [paymentStage, setPaymentStage] = useState<TossCheckoutStage>("idle");
	const [paymentError, setPaymentError] = useState<string | null>(null);
	const requestIdRef = useRef(0);
	const mountedRef = useRef(false);
	const paymentRequestInFlightRef = useRef(false);

	const freePlan = useMemo(
		() => plans.find((plan) => plan.subscriptionLevel === "FREE") ?? null,
		[plans],
	);
	const paidPlan = useMemo(
		() => plans.find((plan) => plan.subscriptionLevel === "PAID") ?? null,
		[plans],
	);

	const isUnsubscribed = isUnsubscribedStatus(subscriptionStatus);
	const isFreeActive = isFreeActiveStatus(subscriptionStatus);
	const isPaidActive = isPaidActiveStatus(subscriptionStatus);
	const isPaidCancelScheduled = isPaidCancelScheduledStatus(subscriptionStatus);
	const isUnexpectedStatus =
		subscriptionStatus !== null &&
		!isUnsubscribed &&
		!isFreeActive &&
		!isPaidActive &&
		!isPaidCancelScheduled;
	const isPaymentBusy = paymentStage !== "idle";
	const paidAmountValid = paidPlan ? isValidPaidAmount(paidPlan) : false;
	const canSelectPaid =
		isAuthenticated &&
		numericCreatorId !== null &&
		creator !== null &&
		!creator.isMine &&
		paidPlan !== null &&
		paidPlan.available &&
		paidAmountValid &&
		!isPaidActive &&
		!isPaidCancelScheduled &&
		!isUnexpectedStatus;
	const canProceedToPayment =
		selectedPlan === "PAID" && canSelectPaid && !isPaymentBusy;

	const loadData = useCallback(async () => {
		if (numericCreatorId === null || !isAuthenticated) return;

		const requestId = requestIdRef.current + 1;
		requestIdRef.current = requestId;
		setLoading(true);
		setError(null);
		setNotice(null);
		setPaymentError(null);
		setPaymentStage("idle");
		paymentRequestInFlightRef.current = false;
		setCreator(null);
		setPlans([]);
		setSubscriptionStatus(null);
		setSelectedPlan(null);

		try {
			const [nextCreator, nextPlans, nextStatus] = await Promise.all([
				wrapLoad(getCreatorProfile(numericCreatorId), "creator"),
				wrapLoad(getSubscriptionPlans(numericCreatorId), "plans"),
				wrapLoad(getSubscriptionStatus(numericCreatorId), "status"),
			]);

			if (!mountedRef.current || requestIdRef.current !== requestId) return;

			setCreator(nextCreator);
			setPlans(nextPlans);
			setSubscriptionStatus(nextStatus);
			setSelectedPlan(getInitialSelectedPlan(nextCreator, nextPlans, nextStatus));
		} catch (err) {
			if (!mountedRef.current || requestIdRef.current !== requestId) return;
			setError(toUserMessage(err));
		} finally {
			if (mountedRef.current && requestIdRef.current === requestId) {
				setLoading(false);
			}
		}
	}, [isAuthenticated, numericCreatorId]);

	useEffect(() => {
		mountedRef.current = true;
		return () => {
			mountedRef.current = false;
			requestIdRef.current += 1;
		};
	}, []);

	useEffect(() => {
		requestIdRef.current += 1;
		setCreator(null);
		setPlans([]);
		setSubscriptionStatus(null);
		setSelectedPlan(null);
		setError(null);
		setNotice(null);
		setPaymentError(null);
		setPaymentStage("idle");
		paymentRequestInFlightRef.current = false;
		setLoading(false);

		if (numericCreatorId === null || !isAuthenticated) return;

		void loadData();
	}, [isAuthenticated, loadData, numericCreatorId]);

	async function handlePaymentStart() {
		if (
			paymentRequestInFlightRef.current ||
			!canProceedToPayment ||
			numericCreatorId === null ||
			!creator ||
			!paidPlan
		) {
			return;
		}

		const clientKey = getConfiguredTossClientKey();
		if (!clientKey) {
			setNotice(null);
			setPaymentError("결제 환경이 설정되지 않았습니다. 관리자에게 문의해 주세요.");
			return;
		}

		paymentRequestInFlightRef.current = true;
		let currentStage: TossCheckoutStage = "idle";

		const moveToStage = (nextStage: TossCheckoutStage) => {
			currentStage = nextStage;
			setPaymentStage(nextStage);
		};

		setPaymentError(null);
		setNotice(null);

		try {
			moveToStage("preparing");
			const prepared = await preparePayment({ creatorId: numericCreatorId });
			const validated = validatePreparedPayment(prepared, {
				expectedAmount: getExpectedPaidAmount(paidPlan),
				expectedCreatorId: numericCreatorId,
				fallbackCreatorNickname: creator.nickname,
			});
			const { successUrl, failUrl } = buildTossRedirectUrls(
				numericCreatorId,
				validated.orderId,
			);

			moveToStage("loading-sdk");
			const tossPayments = await getTossPayments(clientKey);
			const payment = tossPayments.payment({
				customerKey: validated.customerKey,
			});

			moveToStage("requesting-payment");
			await payment.requestPayment({
				method: "CARD",
				amount: {
					currency: "KRW",
					value: validated.amount,
				},
				orderId: validated.orderId,
				orderName: validated.orderName,
				successUrl,
				failUrl,
			});
		} catch (err) {
			if (mountedRef.current) {
				setPaymentError(toPaymentErrorMessage(err, currentStage));
			}
		} finally {
			paymentRequestInFlightRef.current = false;
			if (mountedRef.current) {
				setPaymentStage("idle");
			}
		}
	}

	if (numericCreatorId === null) {
		return (
			<SubscribeSelectShell>
				<Card className="p-6 text-center">
					<h1 className="text-headline-lg font-display text-on-surface">
						잘못된 접근입니다
					</h1>
					<p className="mt-3 text-body-md text-secondary">
						크리에이터 주소를 확인할 수 없습니다.
					</p>
					<div className="mt-6">
						<LinkButton to={paths.home}>홈으로 이동</LinkButton>
					</div>
				</Card>
			</SubscribeSelectShell>
		);
	}

	if (!isAuthenticated) {
		return (
			<SubscribeSelectShell>
				<Card className="p-6">
					<h1 className="text-headline-lg font-display text-on-surface">
						로그인이 필요합니다
					</h1>
					<p className="mt-3 text-body-md text-secondary">
						구독 요금제를 확인하고 결제를 진행하려면 로그인이 필요합니다.
					</p>
					<div className="mt-6 flex flex-col gap-3 sm:flex-row">
						<LinkButton to={paths.login} fullWidth>
							로그인하러 가기
						</LinkButton>
						<LinkButton
							to={paths.creator(numericCreatorId)}
							variant="outline"
							fullWidth
						>
							크리에이터 페이지로
						</LinkButton>
					</div>
				</Card>
			</SubscribeSelectShell>
		);
	}

	if (loading) {
		return (
			<SubscribeSelectShell>
				<Card className="p-8 text-center">
					<p role="status" className="text-body-md text-secondary">
						구독 요금제를 불러오는 중입니다.
					</p>
				</Card>
			</SubscribeSelectShell>
		);
	}

	if (error) {
		return (
			<SubscribeSelectShell>
				<Link
					to={paths.creator(numericCreatorId)}
					className="mb-6 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
				>
					<Icon name="arrow_back" className="text-[18px]" />
					크리에이터 페이지로
				</Link>
				<Card className="p-6">
					<Alert>{error}</Alert>
					<Button
						type="button"
						variant="outline"
						className="mt-4"
						onClick={() => {
							void loadData();
						}}
					>
						다시 시도
					</Button>
				</Card>
			</SubscribeSelectShell>
		);
	}

	if (!creator || !subscriptionStatus) {
		return (
			<SubscribeSelectShell>
				<Card className="p-6 text-center">
					<p className="text-body-md text-secondary">
						구독 정보를 확인할 수 없습니다.
					</p>
					<div className="mt-5">
						<Button
							type="button"
							variant="outline"
							onClick={() => {
								void loadData();
							}}
						>
							다시 시도
						</Button>
					</div>
				</Card>
			</SubscribeSelectShell>
		);
	}

	const statusPeriodEndAtLabel =
		isPaidCancelScheduled && subscriptionStatus.currentPeriodEndAt
			? formatDateTime(subscriptionStatus.currentPeriodEndAt)
			: null;
	const paymentDisabledReason = paidPlan
		? getPaymentDisabledReason({
				creator,
				paidPlan,
				paidAmountValid,
				isPaidActive,
				isPaidCancelScheduled,
				isUnexpectedStatus,
			})
		: "";
	const checkoutNotice = getPaymentStageNotice(paymentStage, notice);
	const checkoutDisabledReason = isPaymentBusy
		? "결제 요청을 처리하는 중입니다."
		: paymentDisabledReason;

	return (
		<SubscribeSelectShell>
			<Link
				to={paths.creator(creator.memberId)}
				className="mb-6 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				{creator.nickname} 프로필로
			</Link>

			<h1 className="mb-2 text-headline-lg font-display text-on-surface">
				{creator.nickname} 작가 {isSupportMode ? "후원하기" : "구독하기"}
			</h1>
			<p className="mb-8 text-body-md text-secondary">
				{isSupportMode
					? "유료 구독으로 창작 활동을 더 응원할 수 있습니다."
					: "서버에 등록된 구독 요금제를 확인하고 다음 결제 단계를 준비합니다."}
			</p>

			<SubscriptionStatusNotice
				label={getStatusLabel(subscriptionStatus)}
				description={getStatusDescription(subscriptionStatus, creator)}
				periodEndAtLabel={statusPeriodEndAtLabel}
			/>

			{plans.length === 0 ? (
				<Card className="mb-8 p-6">
					<p className="text-body-md text-secondary">
						등록된 구독 요금제가 없습니다.
					</p>
				</Card>
			) : (
				<div className="mb-8 grid gap-4 md:grid-cols-2">
					{freePlan && (
						<SubscriptionPlanCard
							plan={freePlan}
							title="무료 구독"
							description={
								freePlan.benefitsDescription ??
								"무료 구독은 크리에이터 페이지에서 시작할 수 있습니다."
							}
							priceLabel={formatPrice(freePlan)}
							current={isFreeActive}
						>
							<LinkButton
								to={paths.creator(creator.memberId)}
								variant="outline"
								fullWidth
							>
								크리에이터 페이지에서 관리
							</LinkButton>
						</SubscriptionPlanCard>
					)}

					{paidPlan ? (
						<SubscriptionPlanCard
							plan={paidPlan}
							title="유료 구독"
							description={paidPlan.benefitsDescription}
							priceLabel={formatPrice(paidPlan)}
							current={isPaidActive || isPaidCancelScheduled}
							selected={selectedPlan === "PAID"}
						>
							<Button
								type="button"
								variant={selectedPlan === "PAID" ? "primary" : "outline"}
								fullWidth
								disabled={!canSelectPaid || isPaymentBusy}
								onClick={() => {
									if (!isPaymentBusy) {
										setSelectedPlan("PAID");
									}
								}}
							>
								{getPaidSelectButtonLabel({
									canSelectPaid,
									creator,
									paidPlan,
									paidAmountValid,
									isPaidActive,
									isPaidCancelScheduled,
								})}
							</Button>
						</SubscriptionPlanCard>
					) : (
						<Card className="p-6">
							<h2 className="text-headline-md font-display text-on-surface">
								유료 구독
							</h2>
							<p className="mt-3 text-body-md text-secondary">
								등록된 유료 요금제가 없습니다.
							</p>
						</Card>
					)}
				</div>
			)}

			{paidPlan && (
				<CheckoutSummary
					creatorNickname={creator.nickname}
					amountLabel={formatPrice(paidPlan)}
					notice={checkoutNotice}
					disabled={!canProceedToPayment}
					disabledReason={checkoutDisabledReason}
					buttonLabel={getPaymentButtonLabel(paymentStage)}
					onReadyClick={() => {
						void handlePaymentStart();
					}}
				/>
			)}
			{paymentError && (
				<div className="mt-4">
					<Alert>{paymentError}</Alert>
				</div>
			)}
		</SubscribeSelectShell>
	);
}

function SubscribeSelectShell({ children }: { children: ReactNode }) {
	return <div className="container-page max-w-3xl py-10">{children}</div>;
}

function toNumericCreatorId(value: string): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isInteger(n) && n > 0 ? n : null;
}

function wrapLoad<T>(promise: Promise<T>, source: LoadErrorSource): Promise<T> {
	return promise.catch((err: unknown) => {
		throw new SubscribeSelectLoadError(source, err);
	});
}

function getInitialSelectedPlan(
	creator: CreatorProfile,
	plans: SubscriptionPlanResponse[],
	status: SubscriptionStatusResponse,
): SelectablePlan {
	const paidPlan = plans.find((plan) => plan.subscriptionLevel === "PAID");
	if (
		creator.isMine ||
		!paidPlan ||
		!paidPlan.available ||
		!isValidPaidAmount(paidPlan) ||
		isPaidActiveStatus(status) ||
		isPaidCancelScheduledStatus(status) ||
		isUnexpectedStatusValue(status)
	) {
		return null;
	}

	return "PAID";
}

function isUnsubscribedStatus(status: SubscriptionStatusResponse | null): boolean {
	return (
		status?.subscribed === false &&
		status.subscriptionLevel === null &&
		status.status === null
	);
}

function isFreeActiveStatus(status: SubscriptionStatusResponse | null): boolean {
	return (
		status?.subscribed === true &&
		status.subscriptionLevel === "FREE" &&
		status.status === "ACTIVE"
	);
}

function isPaidActiveStatus(status: SubscriptionStatusResponse | null): boolean {
	return (
		status?.subscribed === true &&
		status.subscriptionLevel === "PAID" &&
		status.status === "ACTIVE"
	);
}

function isPaidCancelScheduledStatus(
	status: SubscriptionStatusResponse | null,
): boolean {
	return (
		status?.subscribed === true &&
		status.subscriptionLevel === "PAID" &&
		status.status === "CANCEL_SCHEDULED"
	);
}

function isUnexpectedStatusValue(status: SubscriptionStatusResponse): boolean {
	return (
		!isUnsubscribedStatus(status) &&
		!isFreeActiveStatus(status) &&
		!isPaidActiveStatus(status) &&
		!isPaidCancelScheduledStatus(status)
	);
}

function getStatusLabel(status: SubscriptionStatusResponse): string {
	if (isUnsubscribedStatus(status)) return "미구독";
	if (isFreeActiveStatus(status)) return "무료 구독 중";
	if (isPaidActiveStatus(status)) return "유료 구독 중";
	if (isPaidCancelScheduledStatus(status)) return "해지 예약";
	return "상태 확인 필요";
}

function getStatusDescription(
	status: SubscriptionStatusResponse,
	creator: CreatorProfile,
): string {
	if (creator.isMine) {
		return "본인 크리에이터 계정은 구독할 수 없습니다.";
	}
	if (isUnsubscribedStatus(status)) {
		return "아직 구독하지 않았습니다. 무료 구독은 크리에이터 페이지에서 시작할 수 있습니다.";
	}
	if (isFreeActiveStatus(status)) {
		return "현재 무료 구독 중입니다. 유료 구독으로 업그레이드할 수 있습니다.";
	}
	if (isPaidActiveStatus(status)) {
		return "이미 유료 구독 중입니다. 동일한 유료 결제는 진행할 수 없습니다.";
	}
	if (isPaidCancelScheduledStatus(status)) {
		return "유료 구독 해지 예약 상태입니다. 해지 예약 관리는 마이페이지에서 진행합니다.";
	}
	return "현재 구독 상태를 확인할 수 없습니다. 결제 진행이 제한됩니다.";
}

function isValidPaidAmount(plan: SubscriptionPlanResponse): boolean {
	return Number.isFinite(plan.price) && plan.price > 0;
}

function formatPrice(plan: SubscriptionPlanResponse): string {
	if (plan.subscriptionLevel === "FREE") return "무료";
	if (!isValidPaidAmount(plan)) return "가격 확인 필요";
	return `${currencyFormatter.format(plan.price)}원 / 월`;
}

function formatDateTime(value: string): string {
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) return value;
	return dateTimeFormatter.format(date);
}

function getExpectedPaidAmount(plan: SubscriptionPlanResponse): number | null {
	if (!Number.isInteger(plan.price) || plan.price <= 0) {
		return null;
	}

	return plan.price;
}

function getPaymentStageNotice(
	paymentStage: TossCheckoutStage,
	idleNotice: string | null,
): string | null {
	switch (paymentStage) {
		case "preparing":
			return "결제 준비 정보를 요청하고 있습니다.";
		case "loading-sdk":
			return "결제창을 열기 위한 환경을 확인하고 있습니다.";
		case "requesting-payment":
			return "Toss Payments 결제창을 여는 중입니다.";
		default:
			return idleNotice;
	}
}

function getPaymentButtonLabel(paymentStage: TossCheckoutStage): string {
	switch (paymentStage) {
		case "preparing":
			return "결제 준비 중...";
		case "loading-sdk":
		case "requesting-payment":
			return "결제창 여는 중...";
		default:
			return "결제하기";
	}
}

function toPaymentErrorMessage(
	err: unknown,
	paymentStage: TossCheckoutStage,
): string {
	if (err instanceof CheckoutFlowError) {
		return toTossCheckoutErrorInfo(err).message;
	}

	if (paymentStage === "preparing") {
		return toPreparePaymentErrorMessage(err);
	}

	return toTossCheckoutErrorInfo(err).message;
}

function toPreparePaymentErrorMessage(err: unknown): string {
	if (!(err instanceof ApiError)) {
		return "결제 준비 요청에 실패했습니다. 네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
	}

	switch (err.code) {
		case "CREATOR_NOT_FOUND":
		case "CREATOR_PROFILE_NOT_FOUND":
			return "크리에이터 정보를 찾을 수 없습니다.";
		case "SELF_SUBSCRIPTION_NOT_ALLOWED":
			return "본인 크리에이터 계정은 구독할 수 없습니다.";
		case "PAID_SUBSCRIPTION_ALREADY_EXISTS":
			return "이미 유료 구독 중입니다.";
		case "PAYMENT_CONFLICT":
		case "LOCK_ACQUISITION_TIMEOUT":
			return "이미 결제 준비 또는 처리 중입니다. 잠시 후 다시 시도해 주세요.";
		case "PAID_PLAN_NOT_AVAILABLE":
			return "현재 유료 구독 요금제를 사용할 수 없습니다.";
		case "INVALID_REQUEST":
			return "결제 요청 정보가 올바르지 않습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.";
		case "MEMBER_NOT_FOUND":
		case "UNAUTHORIZED":
			return "로그인 상태를 확인한 뒤 다시 시도해 주세요.";
		case "FORBIDDEN":
			return "결제를 진행할 권한을 확인할 수 없습니다.";
		case "INTERNAL_ERROR":
			return "결제 정보를 생성할 수 없습니다. 잠시 후 다시 시도해 주세요.";
		case "UNKNOWN":
			return "결제 준비 결과를 확인하지 못했습니다. 네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
		default:
			return "결제 준비 요청에 실패했습니다. 잠시 후 다시 시도해 주세요.";
	}
}

function getPaidSelectButtonLabel({
	canSelectPaid,
	creator,
	paidPlan,
	paidAmountValid,
	isPaidActive,
	isPaidCancelScheduled,
}: {
	canSelectPaid: boolean;
	creator: CreatorProfile;
	paidPlan: SubscriptionPlanResponse;
	paidAmountValid: boolean;
	isPaidActive: boolean;
	isPaidCancelScheduled: boolean;
}): string {
	if (canSelectPaid) return "유료 요금제 선택";
	if (creator.isMine) return "본인 구독 불가";
	if (isPaidActive) return "이미 유료 구독 중";
	if (isPaidCancelScheduled) return "해지 예약 상태";
	if (!paidPlan.available) return "유료 구독 이용 불가";
	if (!paidAmountValid) return "가격 정보 확인 필요";
	return "선택할 수 없음";
}

function getPaymentDisabledReason({
	creator,
	paidPlan,
	paidAmountValid,
	isPaidActive,
	isPaidCancelScheduled,
	isUnexpectedStatus,
}: {
	creator: CreatorProfile;
	paidPlan: SubscriptionPlanResponse;
	paidAmountValid: boolean;
	isPaidActive: boolean;
	isPaidCancelScheduled: boolean;
	isUnexpectedStatus: boolean;
}): string {
	if (creator.isMine) return "본인 크리에이터 계정은 구독할 수 없습니다.";
	if (isPaidActive) return "이미 유료 구독 중입니다.";
	if (isPaidCancelScheduled) return "해지 예약 상태에서는 새 결제를 진행하지 않습니다.";
	if (!paidPlan.available) return "현재 유료 요금제를 이용할 수 없습니다.";
	if (!paidAmountValid) return "유료 요금제 금액을 확인할 수 없습니다.";
	if (isUnexpectedStatus) return "구독 상태 확인이 필요합니다.";
	return "유료 요금제를 선택할 수 없습니다.";
}

function toUserMessage(err: unknown): string {
	if (err instanceof SubscribeSelectLoadError) {
		return toLoadErrorMessage(err.source, err.original);
	}
	return toLoadErrorMessage("creator", err);
}

function toLoadErrorMessage(source: LoadErrorSource, err: unknown): string {
	if (!(err instanceof ApiError)) {
		return getFallbackLoadMessage(source);
	}

	if (err.code === "UNAUTHORIZED") {
		return "로그인이 만료되었습니다. 다시 로그인해 주세요.";
	}
	if (source === "creator" && err.code === "CREATOR_NOT_FOUND") {
		return "크리에이터를 찾을 수 없습니다.";
	}

	return err.message || getFallbackLoadMessage(source);
}

function getFallbackLoadMessage(source: LoadErrorSource): string {
	switch (source) {
		case "creator":
			return "크리에이터 정보를 불러오지 못했습니다.";
		case "plans":
			return "구독 요금제를 불러오지 못했습니다.";
		case "status":
			return "현재 구독 상태를 불러오지 못했습니다.";
		default:
			return "구독 정보를 불러오지 못했습니다.";
	}
}
