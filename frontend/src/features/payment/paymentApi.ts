import { http, unwrap } from "@/lib/api";
import type {
	PaymentConfirmRequest,
	PaymentConfirmResponse,
	PaymentFailRequest,
	PaymentFailResponse,
	PaymentPrepareRequest,
	PaymentPrepareResponse,
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
