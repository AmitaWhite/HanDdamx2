import type { SubscriptionLevel } from "@/features/subscription/subscriptionApi";

export type PaymentStatus =
	| "PENDING"
	| "CONFIRMING"
	| "SUCCESS"
	| "FAILED"
	| "CANCELED";

export type PaymentCurrency = "KRW";

export interface PaymentPrepareRequest {
	creatorId: number;
}

export interface PaymentPrepareResponse {
	paymentId: number;
	orderId: string;
	idempotencyKey: string;
	customerKey: string;
	creatorId: number;
	creatorNickname: string;
	amount: number;
	currency: PaymentCurrency;
	orderName: string;
}

export interface PaymentConfirmRequest {
	paymentKey: string;
	orderId: string;
	amount: number;
}

export interface PaymentConfirmResponse {
	paymentId: number;
	orderId: string;
	paymentKey: string;
	amount: number;
	status: PaymentStatus;
	paymentMethod: string | null;
	paidAt: string | null;
	subscriptionId: number;
	subscriptionLevel: SubscriptionLevel;
	currentPeriodStartAt: string | null;
	currentPeriodEndAt: string | null;
}

export interface PaymentFailRequest {
	orderId: string;
	code: string;
	message?: string | null;
}

export interface PaymentFailResponse {
	paymentId: number;
	orderId: string;
	status: PaymentStatus;
	failureCode: string | null;
}
