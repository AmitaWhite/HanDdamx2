import { http, unwrap } from "@/lib/api";
import type { SliceResponse } from "@/lib/types";
import type {
	PaymentConfirmRequest,
	PaymentConfirmResponse,
	PaymentDetailResponse,
	PaymentFailRequest,
	PaymentFailResponse,
	MyPaymentsQuery,
	PaymentPrepareRequest,
	PaymentPrepareResponse,
	PaymentSummaryResponse,
} from "./types";

/** Backend: POST /api/payments/prepare */
export function preparePayment(
	request: PaymentPrepareRequest,
): Promise<PaymentPrepareResponse> {
	return unwrap<PaymentPrepareResponse>(http.post("/payments/prepare", request));
}

/** Backend: POST /api/payments/confirm */
export function confirmPayment(
	request: PaymentConfirmRequest,
): Promise<PaymentConfirmResponse> {
	return unwrap<PaymentConfirmResponse>(http.post("/payments/confirm", request));
}

/** Backend: POST /api/payments/fail */
export function failPayment(
	request: PaymentFailRequest,
): Promise<PaymentFailResponse> {
	return unwrap<PaymentFailResponse>(http.post("/payments/fail", request));
}

/** Backend: GET /api/payments/me */
export function getMyPayments({
	page = 0,
	size = 20,
	sort = "createdAt,desc",
}: MyPaymentsQuery = {}): Promise<SliceResponse<PaymentSummaryResponse>> {
	return unwrap<SliceResponse<PaymentSummaryResponse>>(
		http.get("/payments/me", { params: { page, size, sort } }),
	);
}

/** Backend: GET /api/payments/{paymentId} */
export function getPayment(paymentId: number): Promise<PaymentDetailResponse> {
	return unwrap<PaymentDetailResponse>(http.get(`/payments/${paymentId}`));
}
