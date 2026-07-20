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
import { validateEmail } from "@/features/auth/validation";

/** 시안 없음 — 로그인/회원가입과 동일한 톤으로 가볍게 구성. 실제 API 연동은 범위 밖(목데이터). */
export function ForgotPasswordPage() {
	const [email, setEmail] = useState("");
	const [error, setError] = useState<string | undefined>();
	const [sent, setSent] = useState(false);

	function onSubmit(e: FormEvent) {
		e.preventDefault();
		const err = validateEmail(email);
		setError(err ?? undefined);
		if (err) return;
		setSent(true);
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
							onChange={(e) => setEmail(e.target.value)}
							error={error}
						/>
						<Button type="submit" size="lg" fullWidth>
							재설정 링크 보내기
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
