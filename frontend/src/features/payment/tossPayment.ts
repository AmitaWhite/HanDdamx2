import { loadTossPayments, type TossPaymentsSDK } from "@tosspayments/tosspayments-sdk";

import { paths } from "@/app/paths";

import type { PaymentPrepareResponse } from "./types";

const tossPaymentsPromises = new Map<string, Promise<TossPaymentsSDK>>();

export type TossCheckoutStage = "idle" | "preparing" | "loading-sdk" | "requesting-payment";

type CheckoutFlowErrorKind = "config" | "validation" | "sdk" | "request" | "cancel";

export class CheckoutFlowError extends Error {
  kind: CheckoutFlowErrorKind;
  code?: string;

  constructor(kind: CheckoutFlowErrorKind, message: string, code?: string) {
    super(message);
    this.name = "CheckoutFlowError";
    this.kind = kind;
    this.code = code;
  }
}

export interface ValidatedPreparedPayment {
  orderId: string;
  amount: number;
  customerKey: string;
  orderName: string;
}

export interface PreparedPaymentValidationOptions {
  expectedAmount: number | null;
  expectedCreatorId: number;
  fallbackCreatorNickname: string;
}

export interface TossCheckoutErrorInfo {
  code?: string;
  message: string;
}

const ORDER_ID_PATTERN = /^[A-Za-z0-9_\-=]{6,64}$/;
const CUSTOMER_KEY_PATTERN = /^[A-Za-z0-9_\-=.@]{2,50}$/;
const SECRET_KEY_PATTERN = /^(test|live)_sk_/i;

export function getConfiguredTossClientKey(): string | null {
  const clientKey = import.meta.env.VITE_TOSS_CLIENT_KEY?.trim();
  if (!clientKey || SECRET_KEY_PATTERN.test(clientKey)) {
    return null;
  }

  return clientKey;
}

export function getTossPayments(clientKey: string): Promise<TossPaymentsSDK> {
  const cached = tossPaymentsPromises.get(clientKey);
  if (cached) {
    return cached;
  }

  const promise = loadTossPayments(clientKey).catch((error: unknown) => {
    tossPaymentsPromises.delete(clientKey);
    throw error;
  });
  tossPaymentsPromises.set(clientKey, promise);

  return promise;
}

export function validatePreparedPayment(
  prepared: PaymentPrepareResponse,
  options: PreparedPaymentValidationOptions,
): ValidatedPreparedPayment {
  const orderId = prepared.orderId?.trim();
  if (!orderId || !ORDER_ID_PATTERN.test(orderId)) {
    throw new CheckoutFlowError(
      "validation",
      "결제 주문 정보를 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.",
    );
  }

  const amount = Number(prepared.amount);
  if (!Number.isFinite(amount) || !Number.isInteger(amount) || amount <= 0) {
    throw new CheckoutFlowError(
      "validation",
      "결제 금액을 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.",
    );
  }

  if (
    Number.isInteger(options.expectedAmount) &&
    options.expectedAmount !== null &&
    options.expectedAmount > 0 &&
    amount !== options.expectedAmount
  ) {
    throw new CheckoutFlowError(
      "validation",
      "결제 금액이 변경되었습니다. 페이지를 새로고침해 주세요.",
    );
  }

  if (prepared.currency !== "KRW") {
    throw new CheckoutFlowError(
      "validation",
      "결제 통화 정보를 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.",
    );
  }

  if (prepared.creatorId !== options.expectedCreatorId) {
    throw new CheckoutFlowError(
      "validation",
      "결제 대상 정보를 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.",
    );
  }

  const customerKey = prepared.customerKey?.trim();
  if (!customerKey || !CUSTOMER_KEY_PATTERN.test(customerKey)) {
    throw new CheckoutFlowError(
      "validation",
      "결제 고객 정보를 확인할 수 없습니다. 관리자에게 문의해 주세요.",
    );
  }

  const orderName = normalizeOrderName(prepared.orderName, options.fallbackCreatorNickname);

  return {
    orderId,
    amount,
    customerKey,
    orderName,
  };
}

export function buildTossRedirectUrls(creatorId: number, orderId: string) {
  const successUrl = new URL(paths.paymentSuccess, window.location.origin);
  successUrl.searchParams.set("creatorId", String(creatorId));

  const failUrl = new URL(paths.paymentFail, window.location.origin);
  failUrl.searchParams.set("creatorId", String(creatorId));
  failUrl.searchParams.set("orderId", orderId);

  return {
    successUrl: successUrl.toString(),
    failUrl: failUrl.toString(),
  };
}

export function toTossCheckoutErrorInfo(error: unknown): TossCheckoutErrorInfo {
  if (error instanceof CheckoutFlowError) {
    return {
      code: error.code,
      message: error.message,
    };
  }

  const code = getTossErrorCode(error);

  switch (code) {
    case "USER_CANCEL":
    case "PAY_PROCESS_CANCELED":
    case "PAYMENT_REQUEST_ABORTED":
      return {
        code,
        message: "결제가 취소되었습니다. 다시 결제할 수 있습니다.",
      };
    case "NETWORK_ERROR":
      return {
        code,
        message: "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
      };
    case "INVALID_CLIENT_KEY":
    case "NOT_SUPPORTED_WIDGET_KEY":
    case "INSECURE_KEY_USAGE":
      return {
        code,
        message: "결제 환경 설정을 확인할 수 없습니다. 관리자에게 문의해 주세요.",
      };
    case "INVALID_CUSTOMER_KEY":
      return {
        code,
        message: "결제 고객 정보를 확인할 수 없습니다. 관리자에게 문의해 주세요.",
      };
    case "INVALID_AMOUNT_VALUE":
    case "BELOW_ZERO_AMOUNT":
      return {
        code,
        message: "결제 금액을 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.",
      };
    case "INCORRECT_SUCCESS_URL_FORMAT":
    case "INCORRECT_FAIL_URL_FORMAT":
      return {
        code,
        message: "결제 이동 경로를 확인할 수 없습니다. 관리자에게 문의해 주세요.",
      };
    case "INVALID_METHOD_TRANSACTION":
      return {
        code,
        message: "이미 다른 결제 요청이 진행 중입니다. 잠시 후 다시 시도해 주세요.",
      };
    case "INVALID_PARAMETERS":
      return {
        code,
        message: "결제 요청 정보를 확인할 수 없습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요.",
      };
    default:
      return {
        code,
        message: "결제창을 열지 못했습니다. 잠시 후 다시 시도해 주세요.",
      };
  }
}

function normalizeOrderName(orderName: string | null | undefined, fallbackCreatorNickname: string): string {
  const preparedName = sanitizeDisplayText(orderName);
  if (preparedName) {
    return truncate(preparedName, 100);
  }

  const creatorName = sanitizeDisplayText(fallbackCreatorNickname) || "크리에이터";
  return truncate(`${creatorName} 유료 구독`, 100);
}

function sanitizeDisplayText(value: string | null | undefined): string {
  return value?.replace(/[<>]/g, "").replace(/\s+/g, " ").trim() ?? "";
}

function truncate(value: string, maxLength: number): string {
  if (value.length <= maxLength) {
    return value;
  }

  return value.slice(0, maxLength);
}

function getTossErrorCode(error: unknown): string | undefined {
  if (!isObject(error)) {
    return undefined;
  }

  const rawCode = error.code;
  if (typeof rawCode === "string" && rawCode.trim()) {
    return rawCode.trim();
  }

  const rawName = error.name;
  if (typeof rawName === "string") {
    return ERROR_NAME_TO_CODE[rawName];
  }

  return undefined;
}

function isObject(value: unknown): value is { code?: unknown; name?: unknown } {
  return typeof value === "object" && value !== null;
}

const ERROR_NAME_TO_CODE: Record<string, string> = {
  UserCancelError: "USER_CANCEL",
  InvalidClientKeyError: "INVALID_CLIENT_KEY",
  InvalidCustomerKeyError: "INVALID_CUSTOMER_KEY",
  InvalidParametersError: "INVALID_PARAMETERS",
  InvalidAmountValueError: "INVALID_AMOUNT_VALUE",
  BelowZeroAmountError: "BELOW_ZERO_AMOUNT",
  IncorrectSuccessUrlFormatError: "INCORRECT_SUCCESS_URL_FORMAT",
  IncorrectFailUrlFormatError: "INCORRECT_FAIL_URL_FORMAT",
  NotSupportedWidgetKeyError: "NOT_SUPPORTED_WIDGET_KEY",
  InsecureKeyUsageError: "INSECURE_KEY_USAGE",
  InvalidMethodTransactionError: "INVALID_METHOD_TRANSACTION",
  UnknownError: "UNKNOWN",
};
