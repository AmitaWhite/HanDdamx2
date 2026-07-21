import { type FormEvent, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { AuthFormShell } from "@/components/auth/AuthFormShell";
import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { AuthStatusPanel } from "@/components/auth/AuthStatusPanel";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LinkButton } from "@/components/ui/LinkButton";
import { confirmPasswordReset } from "@/features/auth/authApi";
import {PASSWORD_HINT, validatePassword, validatePasswordConfirm,} from "@/features/auth/validation";
import { asApiError } from "@/lib/api";

const INVALID_RESET_TOKEN_CODES = new Set([
	"INVALID_VERIFICATION_TOKEN",
	"EXPIRED_VERIFICATION_TOKEN",
	"ALREADY_PROCESSED_VERIFICATION",
]);

type Status = "form" | "submitting" | "success" | "invalidLink";

// 비밀번호 재설정 메일 링크 착지점
export function ResetPasswordPage() {
	const [searchParams] = useSearchParams();
	const token = searchParams.get("token")?.trim() || null;

	const [status, setStatus] = useState<Status>(token ? "form" : "invalidLink");
	const [newPassword, setNewPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [passwordError, setPasswordError] = useState<string | undefined>();
	const [confirmError, setConfirmError] = useState<string | undefined>();
	const [formError, setFormError] = useState<string | undefined>();

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		if (!token) return;

		setPasswordError(undefined);
		setConfirmError(undefined);
		setFormError(undefined);

		const passwordErr = validatePassword(newPassword);
		const confirmErr = validatePasswordConfirm(newPassword, confirmPassword);
		if (passwordErr || confirmErr) {
			setPasswordError(passwordErr ?? undefined);
			setConfirmError(confirmErr ?? undefined);
			return;
		}

		setStatus("submitting");
		try {
			await confirmPasswordReset(token, newPassword);
			setStatus("success");
		} catch (err) {
			const apiErr = asApiError(err, "비밀번호 재설정에 실패했습니다.");
			if (apiErr.code === "INVALID_REQUEST") {
				setPasswordError(PASSWORD_HINT);
				setStatus("form");
			} else if (INVALID_RESET_TOKEN_CODES.has(apiErr.code)) {
				setStatus("invalidLink");
			} else {
				setFormError(apiErr.message);
				setStatus("form");
			}
		}
	}

	if (status === "invalidLink") {
		return (
			<AuthPageShell>
				<AuthStatusPanel
					variant="error"
					icon="error"
					title="유효하지 않은 링크입니다"
					withCard
					description="링크가 만료되었거나 이미 사용됐습니다. 비밀번호 재설정을 다시 요청해 주세요."
				>
					<LinkButton to={paths.forgotPassword} fullWidth>
						재설정 메일 다시 요청하기
					</LinkButton>
				</AuthStatusPanel>
			</AuthPageShell>
		);
	}

	if (status === "success") {
		return (
			<AuthPageShell>
				<AuthStatusPanel
					variant="primary"
					icon="check_circle"
					title="비밀번호가 변경되었습니다"
					withCard
					description="새 비밀번호로 다시 로그인해 주세요."
				>
					<LinkButton to={paths.login} size="lg" fullWidth>
						로그인하러 가기
					</LinkButton>
				</AuthStatusPanel>
			</AuthPageShell>
		);
	}

	const submitting = status === "submitting";

	return (
		<AuthPageShell>
			<AuthFormShell title="비밀번호 재설정" subtitle="새로 사용할 비밀번호를 입력해 주세요">
				<Card className="w-full p-8 md:p-10">
					{formError && (
						<p className="mb-4 text-body-md text-error">{formError}</p>
					)}
					<form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
						<div>
							<Input
								label="새 비밀번호"
								type="password"
								placeholder="8자 이상 입력"
								autoComplete="new-password"
								value={newPassword}
								disabled={submitting}
								onChange={(e) => {
									setNewPassword(e.target.value);
									setPasswordError(undefined);
									setFormError(undefined);
								}}
								error={passwordError}
							/>
							{!passwordError && (
								<p className="mt-1.5 text-caption font-caption text-secondary">
									{PASSWORD_HINT}
								</p>
							)}
						</div>
						<Input
							label="비밀번호 확인"
							type="password"
							placeholder="비밀번호를 한 번 더 입력해 주세요"
							autoComplete="new-password"
							value={confirmPassword}
							disabled={submitting}
							onChange={(e) => {
								setConfirmPassword(e.target.value);
								setConfirmError(undefined);
								setFormError(undefined);
							}}
							error={confirmError}
						/>
						<Button type="submit" size="lg" fullWidth disabled={submitting}>
							{submitting ? "변경 중…" : "비밀번호 변경"}
						</Button>
					</form>
				</Card>
			</AuthFormShell>
		</AuthPageShell>
	);
}
