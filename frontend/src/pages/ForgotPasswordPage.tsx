import { useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { AuthFormShell } from "@/components/auth/AuthFormShell";
import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { AuthStatusPanel } from "@/components/auth/AuthStatusPanel";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { LinkButton } from "@/components/ui/LinkButton";
import { requestPasswordReset } from "@/features/auth/authApi";
import { validateEmail } from "@/features/auth/validation";
import { asApiError } from "@/lib/api";

/** 시안 없음 — 로그인/회원가입과 동일한 톤으로 가볍게 구성. */
export function ForgotPasswordPage() {
	const [email, setEmail] = useState("");
	const [error, setError] = useState<string | undefined>();
	const [sending, setSending] = useState(false);
	const [sent, setSent] = useState(false);

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		const err = validateEmail(email);
		setError(err ?? undefined);
		if (err) return;

		setSending(true);
		try {
			await requestPasswordReset(email);
			setSent(true);
		} catch (resetErr) {
			setError(
				asApiError(resetErr, "메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.").message,
			);
		} finally {
			setSending(false);
		}
	}

	if (sent) {
		return (
			<AuthPageShell>
				<AuthStatusPanel
					variant="primary"
					icon="mark_email_unread"
					title="재설정 메일을 보냈습니다"
					withCard
					description={
						<>
							<span className="font-bold text-on-surface">{email}</span> 로 보낸 메일의 링크를 눌러 비밀번호를
							재설정해 주세요.
						</>
					}
				>
					<LinkButton to={paths.login} variant="ghost" fullWidth>
						로그인으로 돌아가기
					</LinkButton>
				</AuthStatusPanel>
			</AuthPageShell>
		);
	}

	return (
		<AuthPageShell>
			<AuthFormShell title="비밀번호 찾기" subtitle="가입한 이메일로 재설정 링크를 보내드려요">
				<Card className="w-full p-8 md:p-10">
					<form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
						<Input
							label="이메일"
							type="email"
							autoComplete="email"
							placeholder="example@email.com"
							value={email}
							disabled={sending}
							onChange={(e) => {
								setEmail(e.target.value);
								setError(undefined);
							}}
							error={error}
						/>
						<Button type="submit" size="lg" fullWidth disabled={sending}>
							{sending ? "보내는 중…" : "재설정 링크 보내기"}
						</Button>
					</form>
					<Link
						to={paths.login}
						className="mt-6 block text-center text-label-md font-label-md text-secondary hover:text-primary"
					>
						로그인으로 돌아가기
					</Link>
				</Card>
			</AuthFormShell>
		</AuthPageShell>
	);
}
