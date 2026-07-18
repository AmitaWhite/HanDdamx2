import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { AuthFormShell } from "@/components/auth/AuthFormShell";
import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { useAuth } from "@/features/auth/AuthContext";
import { useSubmitState } from "@/features/auth/useSubmitState";
import { backendOrigin } from "@/lib/api";

export function LoginPage() {
	const { login } = useAuth();
	const navigate = useNavigate();
	const { loading, error, run } = useSubmitState(
		"로그인에 실패했습니다. 다시 시도해 주세요.",
	);

	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");

	async function onSubmit(e: React.FormEvent) {
		e.preventDefault();
		await run(async () => {
			await login(email, password);
			navigate(paths.home, { replace: true });
		});
	}

	function loginWithGoogle() {
		window.location.href = `${backendOrigin}/oauth2/authorization/google`;
	}

	return (
		<AuthPageShell>
			<AuthFormShell title="한땀한땀">
				<Card className="w-full p-8 md:p-10">
					<form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
						{error && <Alert>{error}</Alert>}

						<Input
							label="이메일"
							type="email"
							autoComplete="email"
							placeholder="example@email.com"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							required
						/>
						<Input
							label="비밀번호"
							type="password"
							autoComplete="current-password"
							placeholder="••••••••"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							required
						/>

						<Button
							type="submit"
							size="lg"
							fullWidth
							disabled={loading}
							className="mt-1"
						>
							{loading ? "로그인 중…" : "로그인"}
						</Button>
					</form>

					<div className="mt-8 flex items-center justify-center gap-4 text-label-md font-label-md">
						<Link
							to={paths.forgotPassword}
							className="text-secondary transition-colors hover:text-primary"
						>
							비밀번호 찾기
						</Link>
						<span className="h-3 w-px bg-outline-variant" />
						<Link
							to={paths.signup}
							className="font-bold text-primary hover:underline"
						>
							회원가입
						</Link>
					</div>
				</Card>

				<div className="mt-10 w-full">
					<div className="mb-6 flex items-center gap-4">
						<span className="h-px flex-grow bg-outline-variant" />
						<span className="shrink-0 text-caption font-caption text-secondary">
							또는 간편 로그인
						</span>
						<span className="h-px flex-grow bg-outline-variant" />
					</div>
					<Button
						variant="secondary"
						size="md"
						fullWidth
						onClick={loginWithGoogle}
					>
						<Icon name="account_circle" className="text-[18px]" />
						구글로 계속하기
					</Button>
				</div>
			</AuthFormShell>
		</AuthPageShell>
	);
}
