import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { useAuth } from "@/features/auth/AuthContext";
import { ApiError, backendOrigin } from "@/lib/api";

export function LoginPage() {
	const { login } = useAuth();
	const navigate = useNavigate();

	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [error, setError] = useState<string | null>(null);
	const [loading, setLoading] = useState(false);

	async function onSubmit(e: React.FormEvent) {
		e.preventDefault();
		setError(null);
		setLoading(true);
		try {
			await login(email, password);
			navigate(paths.home, { replace: true });
		} catch (err) {
			setError(
				err instanceof ApiError
					? err.message
					: "로그인에 실패했습니다. 다시 시도해 주세요.",
			);
		} finally {
			setLoading(false);
		}
	}

	function loginWithGoogle() {
		window.location.href = `${backendOrigin}/oauth2/authorization/google`;
	}

	return (
		<div className="flex min-h-[calc(100vh-72px)] items-center justify-center px-margin-mobile py-12">
			<div className="flex w-full max-w-[440px] flex-col items-center">
				{/* 브랜드 */}
				<div className="mb-10 text-center">
					<h1 className="mb-2 text-headline-lg font-display text-on-surface">
						한땀한땀
					</h1>
					<p className="text-body-md text-secondary">
						공예 작가와 팬을 잇는 구독 플랫폼
					</p>
				</div>

				{/* 로그인 카드 */}
				<Card className="w-full p-8 md:p-10">
					<form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
						{error && (
							<p
								role="alert"
								className="rounded bg-error-container px-4 py-3 text-label-md font-label-md text-on-error-container"
							>
								{error}
							</p>
						)}

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

				{/* 간편 로그인 */}
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
			</div>
		</div>
	);
}
