import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useSearchParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { failPayment } from "@/features/payment/paymentApi";
import type {
	PaymentFailRequest,
	PaymentFailResponse,
} from "@/features/payment/types";
import { asApiError, type ApiError } from "@/lib/api";

interface ValidFailQuery {
	params: PaymentFailRequest;
	returnTarget: ReturnTarget;
	retryTarget: ReturnTarget | null;
	displayFailureMessage: string;
}

type FailQueryValidation =
	| { ok: true; value: ValidFailQuery }
	| { ok: false; message: string; returnTarget: ReturnTarget };

type FailViewState =
	| { kind: "idle" }
	| { kind: "processing"; reused: boolean }
	| { kind: "success"; response: PaymentFailResponse }
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

const FAILURE_CODE_MAX_LENGTH = 100;
const FAILURE_MESSAGE_MAX_LENGTH = 500;
const DISPLAY_MESSAGE_MAX_LENGTH = 120;

const failRequests = new Map<string, Promise<PaymentFailResponse>>();

export function PaymentFailPage() {
	const [searchParams] = useSearchParams();
	const queryKey = searchParams.toString();
	const validation = useMemo(
		() => validateFailQuery(new URLSearchParams(queryKey)),
		[queryKey],
	);
	const [attempt, setAttempt] = useState(0);
	const [viewState, setViewState] = useState<FailViewState>({ kind: "idle" });

	useEffect(() => {
		if (!validation.ok) {
			setViewState({ kind: "idle" });
			return;
		}

		let cancelled = false;
		const { promise, reused } = getFailRequest(validation.value.params);

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
						error: toFailErrorInfo(
							asApiError(err, "결제 실패 정보 처리에 실패했습니다."),
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
			<PaymentFailShell>
				<Card className="p-6 text-center md:p-8">
					<ResultIcon tone="error" name="error" />
					<h1 className="mt-5 text-headline-lg font-display text-on-surface">
						잘못된 결제 실패 접근입니다
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
			</PaymentFailShell>
		);
	}

	const { returnTarget, retryTarget, displayFailureMessage } = validation.value;

	if (viewState.kind === "success") {
		return (
			<PaymentFailShell>
				<Card className="p-6 md:p-8">
					<div className="text-center">
						<ResultIcon tone="error" name="cancel" />
						<h1 className="mt-5 text-headline-lg font-display text-on-surface">
							결제가 완료되지 않았습니다
						</h1>
						<p className="mt-3 text-body-md text-secondary">
							결제 실패 정보가 저장되었습니다. 필요한 경우 다시 결제를
							시도할 수 있습니다.
						</p>
					</div>

					<FailureSummary
						orderId={viewState.response.orderId}
						failureMessage={displayFailureMessage}
						statusLabel={formatFailStatus(viewState.response.status)}
					/>

					<div className="mt-7 flex flex-col gap-3 sm:flex-row">
						{retryTarget && (
							<LinkButton to={retryTarget.to} fullWidth>
								{retryTarget.label}
							</LinkButton>
						)}
						<LinkButton
							to={returnTarget.to}
							variant={retryTarget ? "outline" : "primary"}
							fullWidth
						>
							{returnTarget.label}
						</LinkButton>
					</div>
				</Card>
			</PaymentFailShell>
		);
	}

	if (viewState.kind === "error") {
		return (
			<PaymentFailShell>
				<Card className="p-6 md:p-8">
					<div className="text-center">
						<ResultIcon tone="error" name="error" />
						<h1 className="mt-5 text-headline-lg font-display text-on-surface">
							결제 실패 정보 처리에 실패했습니다
						</h1>
						<p className="mt-3 text-body-md text-secondary">
							{viewState.error.title}
						</p>
					</div>

					<FailureSummary
						orderId={validation.value.params.orderId}
						failureMessage={displayFailureMessage}
						statusLabel="실패 정보 처리 오류"
					/>

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
			</PaymentFailShell>
		);
	}

	const reused = viewState.kind === "processing" && viewState.reused;

	return (
		<PaymentFailShell>
			<Card className="p-6 text-center md:p-8">
				<ResultIcon tone="neutral" name="hourglass_empty" />
				<h1 className="mt-5 text-headline-lg font-display text-on-surface">
					결제 실패 정보를 처리 중입니다
				</h1>
				<p role="status" className="mt-3 text-body-md text-secondary">
					{reused
						? "동일한 결제 실패 처리 요청의 결과를 기다리고 있습니다."
						: "결제가 완료되지 않은 사유를 서버에 기록하고 있습니다."}
				</p>
				<div className="mt-6">
					<Button type="button" fullWidth disabled>
						처리 중
					</Button>
				</div>
			</Card>
		</PaymentFailShell>
	);
}

function PaymentFailShell({ children }: { children: ReactNode }) {
	return <div className="container-page max-w-2xl py-12">{children}</div>;
}

function FailureSummary({
	orderId,
	failureMessage,
	statusLabel,
}: {
	orderId: string;
	failureMessage: string;
	statusLabel: string;
}) {
	return (
		<div className="mt-7 divide-y divide-outline-variant/50 rounded border border-outline-variant/60">
			<SummaryRow label="주문 번호" value={orderId} />
			<SummaryRow label="오류 메시지" value={failureMessage} />
			<SummaryRow label="처리 상태" value={statusLabel} />
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
	tone: "error" | "neutral";
	name: string;
}) {
	const toneClass =
		tone === "error"
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

function validateFailQuery(searchParams: URLSearchParams): FailQueryValidation {
	const creatorId = parseOptionalCreatorId(searchParams.get("creatorId"));
	const returnTarget = getReturnTarget(creatorId);
	const retryTarget = getRetryTarget(creatorId);
	const orderId = getRequiredQueryValue(searchParams, "orderId");
	const code = getRequiredQueryValue(searchParams, "code");

	if (!orderId || !code) {
		return {
			ok: false,
			message: "결제 실패 처리에 필요한 정보가 부족합니다.",
			returnTarget,
		};
	}

	const message = normalizeFailureMessage(searchParams.get("message"));
	const params: PaymentFailRequest = {
		orderId,
		code: truncate(code, FAILURE_CODE_MAX_LENGTH),
	};

	if (message) {
		params.message = message;
	}

	return {
		ok: true,
		value: {
			params,
			returnTarget,
			retryTarget,
			displayFailureMessage: getSafeFailureDisplayMessage(
				code,
				searchParams.get("message"),
			),
		},
	};
}

function getFailRequest(params: PaymentFailRequest): {
	promise: Promise<PaymentFailResponse>;
	reused: boolean;
} {
	const requestKey = buildFailRequestKey(params);
	const cached = failRequests.get(requestKey);
	if (cached) {
		return { promise: cached, reused: true };
	}

	const promise = failPayment(params).catch((err: unknown) => {
		failRequests.delete(requestKey);
		throw err;
	});

	failRequests.set(requestKey, promise);
	return { promise, reused: false };
}

function buildFailRequestKey(params: PaymentFailRequest): string {
	return `${params.orderId}\n${params.code}\n${params.message ?? ""}`;
}

function toFailErrorInfo(err: ApiError): PaymentApiErrorInfo {
	switch (err.code) {
		case "INVALID_REQUEST":
			return {
				title: "결제 실패 요청 값을 확인해 주세요.",
				message: "결제 실패 정보가 올바르지 않아 처리할 수 없습니다.",
				retryable: false,
			};
		case "PAYMENT_NOT_FOUND":
			return {
				title: "결제 정보를 찾을 수 없습니다.",
				message: "주문 정보가 없거나 결제 준비가 완료되지 않았습니다.",
				retryable: false,
			};
		case "PAYMENT_CONFLICT":
			return {
				title: "결제 상태가 이미 변경되었습니다.",
				message:
					"이미 완료된 결제이거나 승인 처리 중인 결제라 실패 처리할 수 없습니다.",
				retryable: false,
			};
		case "PAYMENT_NOT_CONFIRMABLE":
			return {
				title: "현재 상태에서는 실패 처리할 수 없습니다.",
				message: "결제 상태가 변경되어 실패 정보를 저장할 수 없습니다.",
				retryable: false,
			};
		case "PAYMENT_ALREADY_FAILED":
			return {
				title: "이미 실패 처리된 결제입니다.",
				message: "이미 실패 정보가 저장된 결제입니다.",
				retryable: false,
			};
		case "LOCK_ACQUISITION_TIMEOUT":
			return {
				title: "동일한 결제 처리가 진행 중입니다.",
				message: "현재 동일한 결제를 처리 중입니다. 잠시 후 다시 시도해 주세요.",
				retryable: true,
			};
		case "PAYMENT_OWNER_MISMATCH":
		case "FORBIDDEN":
			return {
				title: "결제에 접근할 권한이 없습니다.",
				message: "현재 계정으로는 이 결제 실패 정보를 처리할 수 없습니다.",
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
				title: "서버에서 실패 정보를 처리하지 못했습니다.",
				message: "일시적인 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
				retryable: true,
			};
		case "UNKNOWN":
			return {
				title: "결제 실패 처리 결과를 확인하지 못했습니다.",
				message: "네트워크 상태를 확인한 뒤 다시 시도해 주세요.",
				retryable: true,
			};
		default:
			return {
				title: "결제 실패 정보 처리에 실패했습니다.",
				message: "결제 실패 정보를 처리할 수 없습니다. 잠시 후 다시 확인해 주세요.",
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

function getRetryTarget(creatorId: number | null): ReturnTarget | null {
	if (creatorId === null) return null;
	return {
		to: paths.subscribeSelect(creatorId),
		label: "결제 다시 시도",
	};
}

function normalizeFailureMessage(value: string | null): string | undefined {
	if (value === null) return undefined;
	const trimmed = value.trim();
	if (!trimmed) return undefined;
	return truncate(trimmed, FAILURE_MESSAGE_MAX_LENGTH);
}

function getSafeFailureDisplayMessage(
	code: string,
	message: string | null,
): string {
	const codeMessage = getFailureCodeMessage(code);
	if (codeMessage) return codeMessage;

	const safeMessage = sanitizeFailureMessageForDisplay(message);
	if (safeMessage) return safeMessage;

	return "결제가 완료되지 않았습니다. 결제 수단 상태를 확인한 뒤 다시 시도해 주세요.";
}

function getFailureCodeMessage(code: string): string | null {
	switch (code.trim().toUpperCase()) {
		case "PAY_PROCESS_CANCELED":
		case "USER_CANCEL":
			return "사용자가 결제를 취소했습니다.";
		case "NOT_ENOUGH_BALANCE":
			return "잔액 또는 한도가 부족해 결제가 완료되지 않았습니다.";
		case "INVALID_CARD_COMPANY":
		case "INVALID_CARD_NUMBER":
		case "INVALID_CARD_EXPIRATION":
		case "INVALID_CARD_PASSWORD":
		case "INVALID_BIRTH":
			return "결제 수단 정보가 올바르지 않아 결제가 완료되지 않았습니다.";
		case "REJECT_CARD_COMPANY":
		case "REJECT_CARD_PAYMENT":
		case "REJECT_ACCOUNT_PAYMENT":
		case "REJECT_PAYMENT":
			return "결제 수단 승인이 거절되었습니다.";
		default:
			return null;
	}
}

function sanitizeFailureMessageForDisplay(value: string | null): string | null {
	if (value === null) return null;
	const compact = value.replace(/\s+/g, " ").trim();
	if (!compact || compact.length > DISPLAY_MESSAGE_MAX_LENGTH) return null;
	if (/[<>]/.test(compact)) return null;
	if (/https?:\/\/|www\./i.test(compact)) return null;
	if (/(exception|stack trace|traceback|^\s*at\s+\S+\()/i.test(compact)) {
		return null;
	}

	return compact;
}

function truncate(value: string, maxLength: number): string {
	if (value.length <= maxLength) return value;
	return value.slice(0, maxLength);
}

function formatFailStatus(status: string): string {
	if (status === "FAILED") return "실패 정보 저장 완료";
	if (status === "SUCCESS") return "이미 완료된 결제";
	if (status === "CONFIRMING") return "승인 처리 중";
	if (status === "PENDING") return "결제 대기 중";
	return status;
}
