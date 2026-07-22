import { useCallback, useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Alert } from "@/components/ui/Alert";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { getMyPayments, getPayment } from "@/features/payment/paymentApi";
import type {
	PaymentDetailResponse,
	PaymentSummaryResponse,
} from "@/features/payment/types";
import { asApiError } from "@/lib/api";

const PAGE_SIZE = 20;
const PAYMENT_SORT = "createdAt,desc";

const currencyFormatter = new Intl.NumberFormat("ko-KR");
const dateTimeFormatter = new Intl.DateTimeFormat("ko-KR", {
	year: "numeric",
	month: "numeric",
	day: "numeric",
	hour: "2-digit",
	minute: "2-digit",
	hour12: false,
});

export function MyPaymentsPage() {
	const [payments, setPayments] = useState<PaymentSummaryResponse[]>([]);
	const [page, setPage] = useState(0);
	const [hasNext, setHasNext] = useState(false);
	const [loading, setLoading] = useState(true);
	const [loadingMore, setLoadingMore] = useState(false);
	const [initialError, setInitialError] = useState<string | null>(null);
	const [loadMoreError, setLoadMoreError] = useState<string | null>(null);
	const [detailPaymentId, setDetailPaymentId] = useState<number | null>(null);
	const [detailCache, setDetailCache] = useState<
		Map<number, PaymentDetailResponse>
	>(new Map());
	const [detailLoading, setDetailLoading] = useState(false);
	const [detailError, setDetailError] = useState<string | null>(null);

	const mountedRef = useRef(false);
	const initialLoadStartedRef = useRef(false);
	const listRequestIdRef = useRef(0);
	const loadMoreInFlightRef = useRef(false);
	const detailRequestIdRef = useRef(0);
	const detailLoadingPaymentIdRef = useRef<number | null>(null);
	const selectedDetailPaymentIdRef = useRef<number | null>(null);

	const loadInitialPayments = useCallback(async () => {
		const requestId = listRequestIdRef.current + 1;
		listRequestIdRef.current = requestId;
		setLoading(true);
		setInitialError(null);
		setLoadMoreError(null);

		try {
			const slice = await getMyPayments({
				page: 0,
				size: PAGE_SIZE,
				sort: PAYMENT_SORT,
			});
			if (!mountedRef.current || listRequestIdRef.current !== requestId) {
				return;
			}
			setPayments(mergeUniquePayments([], slice.content));
			setPage(slice.page);
			setHasNext(slice.hasNext);
		} catch (err) {
			if (!mountedRef.current || listRequestIdRef.current !== requestId) {
				return;
			}
			setInitialError(
				toErrorMessage(err, "결제 내역을 불러오지 못했습니다."),
			);
		} finally {
			if (mountedRef.current && listRequestIdRef.current === requestId) {
				setLoading(false);
			}
		}
	}, []);

	useEffect(() => {
		mountedRef.current = true;
		if (!initialLoadStartedRef.current) {
			initialLoadStartedRef.current = true;
			void loadInitialPayments();
		}

		return () => {
			mountedRef.current = false;
			selectedDetailPaymentIdRef.current = null;
			detailRequestIdRef.current += 1;
		};
	}, [loadInitialPayments]);

	async function loadMorePayments() {
		if (loadMoreInFlightRef.current || !hasNext) return;

		const nextPage = page + 1;
		const requestId = listRequestIdRef.current + 1;
		listRequestIdRef.current = requestId;
		loadMoreInFlightRef.current = true;
		setLoadingMore(true);
		setLoadMoreError(null);

		try {
			const slice = await getMyPayments({
				page: nextPage,
				size: PAGE_SIZE,
				sort: PAYMENT_SORT,
			});
			if (!mountedRef.current || listRequestIdRef.current !== requestId) {
				return;
			}
			setPayments((prev) => mergeUniquePayments(prev, slice.content));
			setPage(slice.page);
			setHasNext(slice.hasNext);
		} catch (err) {
			if (!mountedRef.current || listRequestIdRef.current !== requestId) {
				return;
			}
			setLoadMoreError(
				toErrorMessage(err, "결제 내역을 더 불러오지 못했습니다."),
			);
		} finally {
			if (mountedRef.current && listRequestIdRef.current === requestId) {
				setLoadingMore(false);
			}
			loadMoreInFlightRef.current = false;
		}
	}

	const loadPaymentDetail = useCallback(async (paymentId: number) => {
		if (detailLoadingPaymentIdRef.current === paymentId) return;

		const requestId = detailRequestIdRef.current + 1;
		detailRequestIdRef.current = requestId;
		detailLoadingPaymentIdRef.current = paymentId;
		setDetailLoading(true);
		setDetailError(null);

		try {
			const detail = await getPayment(paymentId);
			if (
				!mountedRef.current ||
				detailRequestIdRef.current !== requestId ||
				selectedDetailPaymentIdRef.current !== paymentId
			) {
				return;
			}
			setDetailCache((prev) => {
				const next = new Map(prev);
				next.set(paymentId, detail);
				return next;
			});
		} catch (err) {
			if (
				!mountedRef.current ||
				detailRequestIdRef.current !== requestId ||
				selectedDetailPaymentIdRef.current !== paymentId
			) {
				return;
			}
			setDetailError(
				toErrorMessage(err, "결제 상세 정보를 불러오지 못했습니다."),
			);
		} finally {
			if (
				mountedRef.current &&
				detailRequestIdRef.current === requestId &&
				selectedDetailPaymentIdRef.current === paymentId
			) {
				setDetailLoading(false);
				detailLoadingPaymentIdRef.current = null;
			}
		}
	}, []);

	function handleOpenDetail(paymentId: number) {
		selectedDetailPaymentIdRef.current = paymentId;
		setDetailPaymentId(paymentId);
		setDetailError(null);

		if (detailCache.has(paymentId)) {
			setDetailLoading(false);
			detailLoadingPaymentIdRef.current = null;
			return;
		}

		void loadPaymentDetail(paymentId);
	}

	function handleCloseDetail() {
		selectedDetailPaymentIdRef.current = null;
		detailRequestIdRef.current += 1;
		detailLoadingPaymentIdRef.current = null;
		setDetailPaymentId(null);
		setDetailLoading(false);
		setDetailError(null);
	}

	const showInitialError = !loading && payments.length === 0 && initialError;
	const showEmpty = !loading && !initialError && payments.length === 0;
	const selectedSummary =
		detailPaymentId === null
			? undefined
			: payments.find((payment) => payment.paymentId === detailPaymentId);
	const selectedDetail =
		detailPaymentId === null ? undefined : detailCache.get(detailPaymentId);

	return (
		<MyPageShell>
			<div>
				<div className="mb-8">
					<h1 className="text-headline-lg font-display text-on-surface">
						결제 내역
					</h1>
					<p className="mt-1 text-body-md text-secondary">
						유료 구독 결제 상태와 결제 상세 정보를 확인합니다.
					</p>
				</div>

				{loading && payments.length === 0 && (
					<p
						role="status"
						className="py-12 text-center text-body-md text-secondary"
					>
						결제 내역을 불러오는 중입니다.
					</p>
				)}

				{showInitialError && (
					<Card className="p-6 text-center">
						<Alert>{initialError}</Alert>
						<Button
							type="button"
							variant="outline"
							className="mt-5"
							onClick={() => {
								void loadInitialPayments();
							}}
						>
							다시 시도
						</Button>
					</Card>
				)}

				{showEmpty && (
					<div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-outline-variant py-12 text-center">
						<Icon name="payments" className="text-[32px] text-secondary" />
						<p className="text-body-md text-on-surface">
							결제 내역이 없습니다.
						</p>
						<p className="text-caption font-caption text-secondary">
							유료 구독 결제를 완료하면 이곳에서 내역을 확인할 수 있습니다.
						</p>
					</div>
				)}

				{payments.length > 0 && (
					<div className="flex flex-col gap-4">
						{payments.map((payment) => (
							<PaymentCard
								key={payment.paymentId}
								payment={payment}
								onDetailClick={handleOpenDetail}
							/>
						))}
					</div>
				)}

				{payments.length > 0 && hasNext && (
					<div className="mt-6 text-center">
						{loadMoreError && (
							<div className="mb-3">
								<Alert>{loadMoreError}</Alert>
							</div>
						)}
						<Button
							type="button"
							variant="secondary"
							disabled={loadingMore}
							onClick={() => {
								void loadMorePayments();
							}}
						>
							{loadingMore
								? "불러오는 중..."
								: loadMoreError
									? "더보기 다시 시도"
									: "더보기"}
						</Button>
					</div>
				)}
			</div>

			{detailPaymentId !== null && (
				<PaymentDetailOverlay
					paymentId={detailPaymentId}
					summary={selectedSummary}
					detail={selectedDetail}
					loading={detailLoading}
					error={detailError}
					onRetry={() => {
						void loadPaymentDetail(detailPaymentId);
					}}
					onClose={handleCloseDetail}
				/>
			)}
		</MyPageShell>
	);
}

function PaymentCard({
	payment,
	onDetailClick,
}: {
	payment: PaymentSummaryResponse;
	onDetailClick: (paymentId: number) => void;
}) {
	const statusMeta = getStatusMeta(payment.status);

	return (
		<Card className="p-5 md:p-6">
			<div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
				<div className="min-w-0 flex-1">
					<Link
						to={paths.creator(payment.creatorId)}
						className="flex min-w-0 items-center gap-4 rounded-lg transition-opacity hover:opacity-80"
					>
						<Avatar
							src={payment.creatorProfileImageUrl ?? undefined}
							alt={payment.creatorNickname}
							fallbackText={payment.creatorNickname}
							size={56}
						/>
						<div className="min-w-0">
							<p className="truncate text-title-md font-title-md text-on-surface">
								{payment.creatorNickname}
							</p>
							<p className="mt-1 text-body-sm text-secondary">
								크리에이터 페이지로 이동
							</p>
						</div>
					</Link>

					<div className="mt-5 flex flex-wrap items-center gap-2">
						<Chip size="sm" className={statusMeta.className}>
							{statusMeta.label}
						</Chip>
						<Chip size="sm">{formatPaymentMethod(payment.paymentMethod)}</Chip>
					</div>

					<div className="mt-5 grid gap-3 sm:grid-cols-3">
						<InfoItem label="결제 금액" value={formatAmount(payment.amount)} />
						<InfoItem
							label={getPaymentDateLabel(payment)}
							value={getPaymentDateValue(payment)}
						/>
						<InfoItem
							label="결제 수단"
							value={formatPaymentMethod(payment.paymentMethod)}
						/>
					</div>
				</div>

				<div className="flex flex-col gap-2 sm:flex-row lg:w-40 lg:flex-col">
					<LinkButton
						to={paths.creator(payment.creatorId)}
						variant="secondary"
						size="sm"
						fullWidth
					>
						크리에이터 페이지
					</LinkButton>
					<Button
						type="button"
						variant="outline"
						size="sm"
						fullWidth
						onClick={() => onDetailClick(payment.paymentId)}
					>
						상세 보기
					</Button>
				</div>
			</div>
		</Card>
	);
}

function PaymentDetailOverlay({
	paymentId,
	summary,
	detail,
	loading,
	error,
	onRetry,
	onClose,
}: {
	paymentId: number;
	summary?: PaymentSummaryResponse;
	detail?: PaymentDetailResponse;
	loading: boolean;
	error: string | null;
	onRetry: () => void;
	onClose: () => void;
}) {
	const displayPayment = detail ?? summary;

	return (
		<div
			className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
			role="dialog"
			aria-modal="true"
			aria-labelledby="payment-detail-title"
		>
			<Card className="max-h-[90vh] w-full max-w-lg overflow-y-auto p-6">
				<div className="flex items-start justify-between gap-4">
					<div>
						<h2
							id="payment-detail-title"
							className="text-headline-sm font-display text-on-surface"
						>
							결제 상세
						</h2>
						<p className="mt-1 text-body-sm text-secondary">
							결제 상태와 구독 연결 정보를 확인합니다.
						</p>
					</div>
					<Button type="button" variant="ghost" size="sm" onClick={onClose}>
						닫기
					</Button>
				</div>

				{displayPayment && (
					<div className="mt-5 flex items-center gap-3 rounded border border-outline-variant/60 bg-surface-container-low px-4 py-3">
						<Avatar
							src={displayPayment.creatorProfileImageUrl ?? undefined}
							alt={displayPayment.creatorNickname}
							fallbackText={displayPayment.creatorNickname}
							size={44}
						/>
						<div className="min-w-0">
							<p className="truncate text-label-md font-label-md text-on-surface">
								{displayPayment.creatorNickname}
							</p>
							<Link
								to={paths.creator(displayPayment.creatorId)}
								className="text-caption font-caption text-primary hover:underline"
								onClick={onClose}
							>
								크리에이터 페이지로 이동
							</Link>
						</div>
					</div>
				)}

				{loading && (
					<p
						role="status"
						className="py-10 text-center text-body-md text-secondary"
					>
						결제 상세 정보를 불러오는 중입니다.
					</p>
				)}

				{error && (
					<div className="mt-5">
						<Alert>{error}</Alert>
						<Button
							type="button"
							variant="outline"
							className="mt-4"
							disabled={loading}
							onClick={onRetry}
						>
							다시 시도
						</Button>
					</div>
				)}

				{detail && !loading && !error && (
					<div className="mt-5 divide-y divide-outline-variant/50 rounded border border-outline-variant/60">
						<DetailRow label="결제 금액" value={formatAmount(detail.amount)} />
						<DetailRow
							label="결제 상태"
							value={getStatusMeta(detail.status).label}
						/>
						<DetailRow
							label="결제 수단"
							value={formatPaymentMethod(detail.paymentMethod)}
						/>
						<DetailRow
							label={getPaymentDateLabel(detail)}
							value={getPaymentDateValue(detail)}
						/>
						{detail.status === "FAILED" && detail.failureCode && (
							<DetailRow label="실패 코드" value={detail.failureCode} />
						)}
						<DetailRow label="결제 ID" value={String(detail.paymentId)} />
						{detail.subscriptionId !== null && (
							<DetailRow
								label="구독 ID"
								value={String(detail.subscriptionId)}
							/>
						)}
					</div>
				)}

				{!detail && !loading && !error && (
					<p className="py-10 text-center text-body-md text-secondary">
						결제 #{paymentId} 상세 정보를 확인할 수 없습니다.
					</p>
				)}
			</Card>
		</div>
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

function DetailRow({ label, value }: { label: string; value: string }) {
	return (
		<div className="flex items-center justify-between gap-4 px-4 py-3">
			<span className="text-body-md text-secondary">{label}</span>
			<span className="break-all text-right text-body-md font-bold text-on-surface">
				{value}
			</span>
		</div>
	);
}

function getStatusMeta(status: string): { label: string; className: string } {
	switch (status) {
		case "PENDING":
			return {
				label: "결제 대기",
				className: "bg-surface-container text-secondary",
			};
		case "CONFIRMING":
			return {
				label: "승인 처리 중",
				className: "bg-tertiary text-on-tertiary",
			};
		case "SUCCESS":
			return {
				label: "결제 완료",
				className: "bg-primary text-on-primary",
			};
		case "FAILED":
			return {
				label: "결제 실패",
				className: "bg-error-container text-on-error-container",
			};
		case "CANCELED":
			return {
				label: "결제 취소",
				className: "bg-surface-container-high text-secondary",
			};
		default:
			return {
				label: "상태 확인 필요",
				className: "bg-error-container text-on-error-container",
			};
	}
}

function formatAmount(amount: number): string {
	if (!Number.isFinite(amount)) {
		return "금액 확인 필요";
	}

	return `${currencyFormatter.format(amount)}원`;
}

function formatPaymentMethod(value: string | null | undefined): string {
	const trimmed = value?.trim();
	if (!trimmed) return "결제 수단 미확인";

	const methodLabels: Record<string, string> = {
		CARD: "카드",
		EASY_PAY: "간편결제",
		TRANSFER: "계좌이체",
		VIRTUAL_ACCOUNT: "가상계좌",
		MOBILE_PHONE: "휴대폰",
	};
	const normalized = trimmed.toUpperCase();

	return methodLabels[normalized] ?? trimmed;
}

function getPaymentDateLabel(payment: {
	status: string;
	paidAt: string | null;
	createdAt: string;
}): string {
	if (payment.status === "SUCCESS" && parseValidDate(payment.paidAt)) {
		return "결제일";
	}

	return "요청일";
}

function getPaymentDateValue(payment: {
	status: string;
	paidAt: string | null;
	createdAt: string;
}): string {
	const paidAt =
		payment.status === "SUCCESS" ? parseValidDate(payment.paidAt) : null;
	const fallback = parseValidDate(payment.createdAt);
	const date = paidAt ?? fallback;

	return date ? dateTimeFormatter.format(date) : "날짜 미확인";
}

function parseValidDate(value: string | null | undefined): Date | null {
	if (!value) return null;
	const date = new Date(value);
	return Number.isNaN(date.getTime()) ? null : date;
}

function mergeUniquePayments(
	current: PaymentSummaryResponse[],
	incoming: PaymentSummaryResponse[],
): PaymentSummaryResponse[] {
	const seen = new Set<number>();
	const merged: PaymentSummaryResponse[] = [];

	for (const payment of [...current, ...incoming]) {
		if (seen.has(payment.paymentId)) continue;
		seen.add(payment.paymentId);
		merged.push(payment);
	}

	return merged;
}

function toErrorMessage(err: unknown, fallback: string): string {
	const apiError = asApiError(err, fallback);
	return apiError.message || fallback;
}
