import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useSearchParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { confirmPayment } from "@/features/payment/paymentApi";
import type { PaymentConfirmResponse } from "@/features/payment/types";
import { asApiError, type ApiError } from "@/lib/api";

interface ConfirmParams {
	paymentKey: string;
	orderId: string;
	amount: number;
}

interface ValidSuccessQuery {
	params: ConfirmParams;
	creatorId: number | null;
	returnTarget: ReturnTarget;
}

type SuccessQueryValidation =
	| { ok: true; value: ValidSuccessQuery }
	| { ok: false; message: string; returnTarget: ReturnTarget };

type ConfirmViewState =
	| { kind: "idle" }
	| { kind: "processing"; reused: boolean }
	| { kind: "success"; response: PaymentConfirmResponse }
	| { kind: "error"; error: PaymentApiErrorInfo };

interface PaymentApiErrorInfo {
	title: string;
	message: string;
	retryable: boolean;
}

interface ReturnTarget {
	to: string;
	label: string;
}

const MAX_BACKEND_INTEGER = 2_147_483_647;

const currencyFormatter = new Intl.NumberFormat("ko-KR");
const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
	year: "numeric",
	month: "long",
	day: "numeric",
	hour: "2-digit",
	minute: "2-digit",
});

const confirmRequests = new Map<string, Promise<PaymentConfirmResponse>>();

export function PaymentSuccessPage() {
	const [searchParams] = useSearchParams();
	const queryKey = searchParams.toString();
	const validation = useMemo(
		() => validateSuccessQuery(new URLSearchParams(queryKey)),
		[queryKey],
	);
	const [attempt, setAttempt] = useState(0);
	const [viewState, setViewState] = useState<ConfirmViewState>({
		kind: "idle",
	});

	useEffect(() => {
		if (!validation.ok) {
			setViewState({ kind: "idle" });
			return;
		}

		let cancelled = false;
		const { promise, reused } = getConfirmRequest(validation.value.params);

		setViewState({ kind: "processing", reused });
		promise
			.then((response) => {
				if (!cancelled) {
					setViewState({ kind: "success", response });
				}
			})
			.catch((err: unknown) => {
				if (!cancelled) {
					setViewState({
						kind: "error",
						error: toConfirmErrorInfo(
							asApiError(err, "결제 승인 처리에 실패했습니다."),
						),
					});
				}
			});

		return () => {
			cancelled = true;
		};
	}, [attempt, validation]);

	function handleRetry() {
		if (
			!validation.ok ||
			viewState.kind !== "error" ||
			!viewState.error.retryable
		) {
			return;
		}

		setViewState({ kind: "processing", reused: false });
		setAttempt((value) => value + 1);
	}

	if (!validation.ok) {
		return (
			<PaymentSuccessShell>
				<Card className="p-6 text-center md:p-8">
					<ResultIcon tone="error" name="error" />
					<h1 className="mt-5 text-headline-lg font-display text-on-surface">
						잘못된 결제 접근입니다
					</h1>
					<p className="mt-3 text-body-md text-secondary">
						{validation.message}
					</p>
					<div className="mt-6">
						<LinkButton to={validation.returnTarget.to} fullWidth>
							{validation.returnTarget.label}
						</LinkButton>
					</div>
				</Card>
			</PaymentSuccessShell>
		);
	}

	const returnTarget = validation.value.returnTarget;

	if (viewState.kind === "success") {
		return (
			<PaymentSuccessShell>
				<Card className="p-6 md:p-8">
					<div className="text-center">
						<ResultIcon tone="primary" name="check_circle" />
						<h1 className="mt-5 text-headline-lg font-display text-on-surface">
							결제가 완료되었습니다
						</h1>
						<p className="mt-3 text-body-md text-secondary">
							유료 구독이 활성화되었습니다. 승인 결과를 아래에서 확인할 수
							있습니다.
						</p>
					</div>

					<PaymentSummary response={viewState.response} />

					<div className="mt-7">
						<LinkButton to={returnTarget.to} fullWidth>
							{returnTarget.label}
						</LinkButton>
					</div>
				</Card>
			</PaymentSuccessShell>
		);
	}

	if (viewState.kind === "error") {
		return (
			<PaymentSuccessShell>
				<Card className="p-6 md:p-8">
					<div className="text-center">
						<ResultIcon tone="error" name="error" />
						<h1 className="mt-5 text-headline-lg font-display text-on-surface">
							결제 승인에 실패했습니다
						</h1>
						<p className="mt-3 text-body-md text-secondary">
							{viewState.error.title}
						</p>
					</div>

					<div className="mt-6">
						<Alert>{viewState.error.message}</Alert>
					</div>

					<div className="mt-6 flex flex-col gap-3 sm:flex-row">
						{viewState.error.retryable && (
							<Button
								type="button"
								variant="outline"
								fullWidth
								onClick={handleRetry}
							>
								다시 시도
							</Button>
						)}
						<LinkButton to={returnTarget.to} fullWidth>
							{returnTarget.label}
						</LinkButton>
					</div>
				</Card>
			</PaymentSuccessShell>
		);
	}

	const reused = viewState.kind === "processing" && viewState.reused;

	return (
		<PaymentSuccessShell>
			<Card className="p-6 text-center md:p-8">
				<ResultIcon tone="neutral" name="hourglass_empty" />
				<h1 className="mt-5 text-headline-lg font-display text-on-surface">
					결제 승인 처리 중입니다
				</h1>
				<p role="status" className="mt-3 text-body-md text-secondary">
					{reused
						? "동일한 결제 승인 요청의 처리 결과를 기다리고 있습니다."
						: "Toss Payments 승인 결과를 서버에 확인하고 있습니다."}
				</p>
				<div className="mt-6">
					<Button type="button" fullWidth disabled>
						처리 중
					</Button>
				</div>
			</Card>
		</PaymentSuccessShell>
	);
}

function PaymentSuccessShell({ children }: { children: ReactNode }) {
	return <div className="container-page max-w-2xl py-12">{children}</div>;
}

function PaymentSummary({ response }: { response: PaymentConfirmResponse }) {
	const periodLabel = formatPeriod(
		response.currentPeriodStartAt,
		response.currentPeriodEndAt,
	);
	const paidAtLabel = formatDateTime(response.paidAt);
	const paymentMethod = response.paymentMethod?.trim();

	return (
		<div className="mt-7 divide-y divide-outline-variant/50 rounded border border-outline-variant/60">
			<SummaryRow label="주문 번호" value={response.orderId} />
			<SummaryRow label="결제 금액" value={formatAmount(response.amount)} />
			{paymentMethod && <SummaryRow label="결제 수단" value={paymentMethod} />}
			{paidAtLabel && <SummaryRow label="승인 일시" value={paidAtLabel} />}
			<SummaryRow
				label="구독 상태"
				value={`${formatSubscriptionLevel(response.subscriptionLevel)} 활성화`}
			/>
			{periodLabel && <SummaryRow label="구독 기간" value={periodLabel} />}
		</div>
	);
}

function SummaryRow({ label, value }: { label: string; value: string }) {
	return (
		<div className="flex items-center justify-between gap-4 px-4 py-3">
			<span className="text-body-md text-secondary">{label}</span>
			<span className="text-right text-body-md font-bold text-on-surface">
				{value}
			</span>
		</div>
	);
}

function ResultIcon({
	tone,
	name,
}: {
	tone: "primary" | "error" | "neutral";
	name: string;
}) {
	const toneClass =
		tone === "primary"
			? "bg-primary/10 text-primary"
			: tone === "error"
				? "bg-error-container text-on-error-container"
				: "bg-surface-container text-secondary";

	return (
		<div
			className={`mx-auto flex h-16 w-16 items-center justify-center rounded-full ${toneClass}`}
		>
			<Icon name={name} className="text-[32px]" />
		</div>
	);
}

function validateSuccessQuery(
	searchParams: URLSearchParams,
): SuccessQueryValidation {
	const creatorId = parseOptionalCreatorId(searchParams.get("creatorId"));
	const returnTarget = getReturnTarget(creatorId);
	const paymentKey = getRequiredQueryValue(searchParams, "paymentKey");
	const orderId = getRequiredQueryValue(searchParams, "orderId");
	const amountText = getRequiredQueryValue(searchParams, "amount");

	if (!paymentKey || !orderId || !amountText) {
		return {
			ok: false,
			message: "결제 승인에 필요한 정보가 부족합니다.",
			returnTarget,
		};
	}

	const amount = Number(amountText);
	if (
		!Number.isFinite(amount) ||
		!Number.isInteger(amount) ||
		amount <= 0 ||
		amount > MAX_BACKEND_INTEGER
	) {
		return {
			ok: false,
			message: "결제 금액 정보가 올바르지 않습니다.",
			returnTarget,
		};
	}

	return {
		ok: true,
		value: {
			params: { paymentKey, orderId, amount },
			creatorId,
			returnTarget,
		},
	};
}

function getConfirmRequest(params: ConfirmParams): {
	promise: Promise<PaymentConfirmResponse>;
	reused: boolean;
} {
	const requestKey = buildConfirmRequestKey(params);
	const cached = confirmRequests.get(requestKey);
	if (cached) {
		return { promise: cached, reused: true };
	}

	const promise = confirmPayment({
		paymentKey: params.paymentKey,
		orderId: params.orderId,
		amount: params.amount,
	}).catch((err: unknown) => {
		confirmRequests.delete(requestKey);
		throw err;
	});

	confirmRequests.set(requestKey, promise);
	return { promise, reused: false };
}

function buildConfirmRequestKey(params: ConfirmParams): string {
	return `${params.paymentKey}\n${params.orderId}\n${params.amount}`;
}

function toConfirmErrorInfo(err: ApiError): PaymentApiErrorInfo {
	switch (err.code) {
		case "INVALID_REQUEST":
			return {
				title: "결제 승인 요청 값을 확인해 주세요.",
				message: "결제 승인 정보가 올바르지 않아 승인을 진행할 수 없습니다.",
				retryable: false,
			};
		case "PAYMENT_NOT_FOUND":
			return {
				title: "결제 정보를 찾을 수 없습니다.",
				message: "주문 정보가 없거나 결제 준비가 완료되지 않았습니다.",
				retryable: false,
			};
		case "PAYMENT_AMOUNT_MISMATCH":
			return {
				title: "결제 금액이 일치하지 않습니다.",
				message: "서버에 저장된 결제 금액과 승인 요청 금액이 다릅니다.",
				retryable: false,
			};
		case "PAYMENT_NOT_CONFIRMABLE":
			return {
				title: "현재 상태에서는 결제를 승인할 수 없습니다.",
				message: "결제 상태가 변경되었거나 승인 정보가 올바르지 않습니다.",
				retryable: false,
			};
		case "PAYMENT_ALREADY_FAILED":
			return {
				title: "이미 실패 처리된 결제입니다.",
				message: "실패 처리된 결제는 다시 승인할 수 없습니다.",
				retryable: false,
			};
		case "PAYMENT_CONFLICT":
			return {
				title: "결제 상태를 확인하고 있습니다.",
				message: "현재 동일한 결제를 처리 중입니다. 잠시 후 다시 시도해 주세요.",
				retryable: true,
			};
		case "LOCK_ACQUISITION_TIMEOUT":
			return {
				title: "동일한 결제가 처리 중입니다.",
				message: "현재 동일한 결제를 처리 중입니다. 잠시 후 다시 시도해 주세요.",
				retryable: true,
			};
		case "DUPLICATE_PAYMENT_KEY":
			return {
				title: "이미 사용된 결제 정보입니다.",
				message: "다른 주문에 이미 사용된 결제 정보로는 승인할 수 없습니다.",
				retryable: false,
			};
		case "TOSS_CONFIRM_FAILED":
			return {
				title: "PG 승인에 실패했습니다.",
				message: "결제 승인사가 결제를 승인하지 않았습니다.",
				retryable: false,
			};
		case "TOSS_API_ERROR":
			return {
				title: "결제 승인사 응답을 확인하지 못했습니다.",
				message: "Toss Payments 승인 요청 처리 중 문제가 발생했습니다.",
				retryable: true,
			};
		case "TOSS_TIMEOUT":
			return {
				title: "결제 승인 요청이 지연되고 있습니다.",
				message: "Toss Payments 응답이 지연되고 있습니다. 잠시 후 다시 시도해 주세요.",
				retryable: true,
			};
		case "INVALID_TOSS_RESPONSE":
			return {
				title: "결제 승인 응답을 확인할 수 없습니다.",
				message: "승인사 응답이 올바르지 않아 결제 결과를 확정하지 못했습니다.",
				retryable: false,
			};
		case "PAID_SUBSCRIPTION_ALREADY_EXISTS":
			return {
				title: "이미 유료 구독이 활성화되어 있습니다.",
				message: "현재 이용 중인 유료 구독이 있어 새 결제를 승인할 수 없습니다.",
				retryable: false,
			};
		case "PAID_PLAN_NOT_AVAILABLE":
			return {
				title: "유료 구독 요금제를 사용할 수 없습니다.",
				message: "현재 선택한 유료 구독 요금제로 결제를 진행할 수 없습니다.",
				retryable: false,
			};
		case "PAYMENT_OWNER_MISMATCH":
		case "FORBIDDEN":
			return {
				title: "결제에 접근할 권한이 없습니다.",
				message: "현재 계정으로는 이 결제 정보를 승인할 수 없습니다.",
				retryable: false,
			};
		case "UNAUTHORIZED":
			return {
				title: "로그인이 필요합니다.",
				message: "로그인 상태를 확인한 뒤 다시 시도해 주세요.",
				retryable: false,
			};
		case "INTERNAL_ERROR":
			return {
				title: "서버에서 결제 승인 결과를 처리하지 못했습니다.",
				message: "일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
				retryable: true,
			};
		case "UNKNOWN":
			return {
				title: "결제 승인 결과를 확인하지 못했습니다.",
				message: "네트워크 상태를 확인한 뒤 다시 시도해 주세요.",
				retryable: true,
			};
		default:
			return {
				title: "결제 승인 처리에 실패했습니다.",
				message: "결제 승인 결과를 처리할 수 없습니다. 잠시 후 다시 확인해 주세요.",
				retryable: false,
			};
	}
}

function getRequiredQueryValue(
	searchParams: URLSearchParams,
	name: string,
): string | null {
	const value = searchParams.get(name)?.trim();
	return value ? value : null;
}

function parseOptionalCreatorId(value: string | null): number | null {
	if (value === null) return null;
	const trimmed = value.trim();
	if (!trimmed) return null;
	const numeric = Number(trimmed);
	return Number.isInteger(numeric) && numeric > 0 ? numeric : null;
}

function getReturnTarget(creatorId: number | null): ReturnTarget {
	if (creatorId !== null) {
		return {
			to: paths.creator(creatorId),
			label: "크리에이터 페이지로 이동",
		};
	}

	return {
		to: paths.home,
		label: "홈으로 이동",
	};
}

function formatAmount(amount: number): string {
	if (!Number.isFinite(amount)) {
		return "금액 확인 필요";
	}

	return `${currencyFormatter.format(amount)}원`;
}

function formatDateTime(value: string | null): string | null {
	if (!value) return null;
	const date = new Date(value);
	if (Number.isNaN(date.getTime())) {
		return value;
	}

	return dateTimeFormatter.format(date);
}

function formatPeriod(startAt: string | null, endAt: string | null): string | null {
	const start = formatDateTime(startAt);
	const end = formatDateTime(endAt);

	if (start && end) return `${start} - ${end}`;
	if (start) return `${start}부터`;
	if (end) return `${end}까지`;
	return null;
}

function formatSubscriptionLevel(level: string): string {
	if (level === "PAID") return "유료 구독";
	if (level === "FREE") return "무료 구독";
	return level;
}
