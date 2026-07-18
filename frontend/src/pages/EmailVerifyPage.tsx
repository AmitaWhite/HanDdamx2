import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { AuthStatusPanel } from "@/components/auth/AuthStatusPanel";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { confirmEmailVerification } from "@/features/auth/authApi";
import { useResendVerification } from "@/features/auth/useResendVerification";
import { validateEmail } from "@/features/auth/validation";

type Status = "verifying" | "success" | "failed";

/**
 * 인증 메일 링크 착지점: {frontend}/email-verify?token=...
 * 토큰으로 이메일 인증을 확정한다. (토큰 유효기간 10분)
 */
export function EmailVerifyPage() {
	const [searchParams] = useSearchParams();
	const token = searchParams.get("token");

	const [status, setStatus] = useState<Status>(token ? "verifying" : "failed");
	const ran = useRef(false);

	// 재발송용
	const [email, setEmail] = useState("");
	const [emailError, setEmailError] = useState<string | undefined>();
	const { status: resend, resend: resendEmail } = useResendVerification();

	useEffect(() => {
		if (!token || ran.current) return;
		ran.current = true; // StrictMode 이중 실행 방지
		confirmEmailVerification(token)
			.then(() => setStatus("success"))
			.catch(() => setStatus("failed"));
	}, [token]);

	function onResend() {
		const err = validateEmail(email);
		setEmailError(err ?? undefined);
		if (err) return;
		resendEmail(email);
	}

	if (status === "verifying") {
		return (
			<AuthPageShell>
				<AuthStatusPanel
					variant="loading"
					description="이메일 인증 중…"
					withCard
				/>
			</AuthPageShell>
		);
	}

	if (status === "success") {
		return (
			<AuthPageShell>
				<AuthStatusPanel
					variant="primary"
					icon="check_circle"
					title="이메일 인증이 완료되었습니다"
					description="이제 로그인하고 이용하실 수 있습니다."
					withCard
				>
					<Link to={paths.login}>
						<Button size="lg" fullWidth>
							로그인하러 가기
						</Button>
					</Link>
				</AuthStatusPanel>
			</AuthPageShell>
		);
	}

	// status === "failed"
	return (
		<AuthPageShell>
			<AuthStatusPanel
				variant="error"
				icon="error"
				title="인증에 실패했습니다"
				withCard
				description={
					<>
						링크가 만료되었거나 올바르지 않습니다. 링크는 발송 후 10분간만
						유효합니다.
						<br />
						아래에 가입한 이메일을 입력하면 인증 메일을 다시 보내드립니다.
					</>
				}
			>
				{resend === "sent" ? (
					<p className="mb-4 text-label-md font-label-md text-primary">
						인증 메일을 다시 보냈습니다. 메일함을 확인해 주세요.
					</p>
				) : (
					<div className="mb-4 flex flex-col gap-3 text-left">
						<Input
							label="이메일"
							type="email"
							placeholder="example@email.com"
							autoComplete="email"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							error={emailError}
						/>
						{resend === "error" && (
							<p className="text-caption font-caption text-error">
								재발송에 실패했습니다. 잠시 후 다시 시도해 주세요.
							</p>
						)}
						<Button
							fullWidth
							onClick={onResend}
							disabled={resend === "sending"}
						>
							{resend === "sending" ? "보내는 중…" : "인증 메일 다시 보내기"}
						</Button>
					</div>
				)}

				<Link
					to={paths.login}
					className="text-label-md font-label-md text-secondary hover:text-primary"
				>
					로그인으로 돌아가기
				</Link>
			</AuthStatusPanel>
		</AuthPageShell>
	);
}
