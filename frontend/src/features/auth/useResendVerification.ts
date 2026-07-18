import { useState } from "react";
import { resendVerificationEmail } from "./authApi";

export type ResendStatus = "idle" | "sending" | "sent" | "error";

/** 인증 메일 재발송 상태 전이(idle→sending→sent/error). 이메일 입력값/검증은 호출부가 소유한다. */
export function useResendVerification() {
	const [status, setStatus] = useState<ResendStatus>("idle");

	async function resend(email: string) {
		setStatus("sending");
		try {
			await resendVerificationEmail(email);
			setStatus("sent");
		} catch {
			setStatus("error");
		}
	}

	return { status, resend };
}
