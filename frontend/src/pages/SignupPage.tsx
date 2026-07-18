import { useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { AuthFormShell } from "@/components/auth/AuthFormShell";
import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { AuthStatusPanel } from "@/components/auth/AuthStatusPanel";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import {
	checkEmailAvailability,
	checkNicknameAvailability,
	signupRequest,
} from "@/features/auth/authApi";
import { useResendVerification } from "@/features/auth/useResendVerification";
import { useSubmitState } from "@/features/auth/useSubmitState";
import {
	PASSWORD_HINT,
	validateEmail,
	validateNickname,
	validatePassword,
	validatePasswordConfirm,
} from "@/features/auth/validation";

type Errors = Partial<
	Record<"nickname" | "email" | "password" | "passwordConfirm", string>
>;

export function SignupPage() {
	const [nickname, setNickname] = useState("");
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [passwordConfirm, setPasswordConfirm] = useState("");

	const [errors, setErrors] = useState<Errors>({});
	const {
		loading: submitting,
		error: serverError,
		run,
	} = useSubmitState("회원가입에 실패했습니다. 다시 시도해 주세요.");

	/** 가입 성공 시 인증 안내 화면으로 전환 (가입한 이메일 보관) */
	const [signedUpEmail, setSignedUpEmail] = useState<string | null>(null);
	const { status: resend, resend: resendEmail } = useResendVerification();

	function setFieldError(field: keyof Errors, message?: string) {
		setErrors((prev) => ({ ...prev, [field]: message }));
	}

	/** blur 시: 형식 검증 → 통과하면 중복 확인 */
	async function handleEmailBlur() {
		const err = validateEmail(email);
		setFieldError("email", err ?? undefined);
		if (err) return;
		try {
			const { available } = await checkEmailAvailability(email);
			if (!available) setFieldError("email", "이미 사용 중인 이메일입니다.");
		} catch {
			// 중복확인 실패는 무시 — 제출 시 서버가 최종 판정
		}
	}

	async function handleNicknameBlur() {
		const err = validateNickname(nickname);
		setFieldError("nickname", err ?? undefined);
		if (err) return;
		try {
			const { available } = await checkNicknameAvailability(nickname);
			if (!available) setFieldError("nickname", "이미 사용 중인 닉네임입니다.");
		} catch {
			// 무시
		}
	}

	async function onSubmit(e: React.FormEvent) {
		e.preventDefault();

		const next: Errors = {
			nickname: validateNickname(nickname) ?? undefined,
			email: validateEmail(email) ?? undefined,
			password: validatePassword(password) ?? undefined,
			passwordConfirm:
				validatePasswordConfirm(password, passwordConfirm) ?? undefined,
		};
		setErrors(next);
		if (Object.values(next).some(Boolean)) return;

		await run(async () => {
			await signupRequest(email, password, nickname);
			setSignedUpEmail(email);
		});
	}

	// --- 가입 완료: 이메일 인증 안내 ---
	if (signedUpEmail) {
		return (
			<AuthPageShell>
				<AuthStatusPanel
					variant="primary"
					icon="mark_email_unread"
					title="인증 메일을 보냈습니다"
					withCard
					description={
						<>
							<span className="font-bold text-on-surface">{signedUpEmail}</span>{" "}
							로 보낸 메일의 링크를 눌러 인증을 완료해 주세요.
							<br />
							링크는 <span className="font-bold">10분</span> 동안 유효합니다.
						</>
					}
				>
					{resend === "sent" && (
						<p className="mb-4 text-label-md font-label-md text-primary">
							인증 메일을 다시 보냈습니다.
						</p>
					)}
					{resend === "error" && (
						<p className="mb-4 text-label-md font-label-md text-error">
							재발송에 실패했습니다. 잠시 후 다시 시도해 주세요.
						</p>
					)}

					<div className="flex flex-col gap-3">
						<Button
							variant="secondary"
							fullWidth
							onClick={() => resendEmail(signedUpEmail)}
							disabled={resend === "sending"}
						>
							{resend === "sending" ? "보내는 중…" : "인증 메일 다시 보내기"}
						</Button>
						<Link to={paths.login}>
							<Button variant="ghost" fullWidth>
								로그인하러 가기
							</Button>
						</Link>
					</div>
				</AuthStatusPanel>
			</AuthPageShell>
		);
	}

	// --- 가입 폼 ---
	return (
		<AuthPageShell>
			<AuthFormShell title="회원가입">
				<Card className="w-full p-8 md:p-10">
					<form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
						{serverError && <Alert>{serverError}</Alert>}

						<Input
							label="닉네임"
							placeholder="한땀장인"
							autoComplete="nickname"
							value={nickname}
							onChange={(e) => setNickname(e.target.value)}
							onBlur={handleNicknameBlur}
							error={errors.nickname}
						/>
						<Input
							label="이메일"
							type="email"
							placeholder="example@email.com"
							autoComplete="email"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							onBlur={handleEmailBlur}
							error={errors.email}
						/>
						<div>
							<Input
								label="비밀번호"
								type="password"
								placeholder="8자 이상 입력"
								autoComplete="new-password"
								value={password}
								onChange={(e) => setPassword(e.target.value)}
								onBlur={() =>
									setFieldError(
										"password",
										validatePassword(password) ?? undefined,
									)
								}
								error={errors.password}
							/>
							{!errors.password && (
								<p className="mt-1.5 text-caption font-caption text-secondary">
									{PASSWORD_HINT}
								</p>
							)}
						</div>
						<Input
							label="비밀번호 확인"
							type="password"
							placeholder="비밀번호 재입력"
							autoComplete="new-password"
							value={passwordConfirm}
							onChange={(e) => setPasswordConfirm(e.target.value)}
							onBlur={() =>
								setFieldError(
									"passwordConfirm",
									validatePasswordConfirm(password, passwordConfirm) ??
										undefined,
								)
							}
							error={errors.passwordConfirm}
						/>

						<Button
							type="submit"
							size="lg"
							fullWidth
							disabled={submitting}
							className="mt-1"
						>
							{submitting ? "가입 중…" : "회원가입"}
						</Button>
					</form>

					<p className="mt-8 text-center text-label-md font-label-md text-secondary">
						이미 계정이 있으신가요?{" "}
						<Link
							to={paths.login}
							className="font-bold text-primary hover:underline"
						>
							로그인
						</Link>
					</p>
				</Card>
			</AuthFormShell>
		</AuthPageShell>
	);
}
