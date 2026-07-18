import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { AuthPageShell } from "@/components/auth/AuthPageShell";
import { AuthStatusPanel } from "@/components/auth/AuthStatusPanel";
import { useAuth } from "@/features/auth/AuthContext";

/**
 * 구글 OAuth 성공 후 백엔드가 리다이렉트하는 착지점(/oauth/callback).
 * refresh 쿠키로 access token 을 발급받아 로그인을 완료한다.
 */
export function OAuthCallbackPage() {
	const { completeOAuth } = useAuth();
	const navigate = useNavigate();
	const [failed, setFailed] = useState(false);
	const ran = useRef(false);

	useEffect(() => {
		if (ran.current) return; // StrictMode 이중 실행 방지
		ran.current = true;
		completeOAuth()
			.then(() => navigate(paths.home, { replace: true }))
			.catch(() => setFailed(true));
	}, [completeOAuth, navigate]);

	if (failed) {
		return (
			<AuthPageShell padded={false} className="flex-col gap-4 text-center">
				<p className="text-body-md text-on-surface">
					구글 로그인에 실패했습니다.
				</p>
				<Link
					to={paths.login}
					className="text-label-md font-label-md font-bold text-primary hover:underline"
				>
					로그인으로 돌아가기
				</Link>
			</AuthPageShell>
		);
	}

	return (
		<AuthPageShell padded={false} className="flex-col gap-4 text-center">
			<AuthStatusPanel variant="loading" description="로그인 처리 중…" />
		</AuthPageShell>
	);
}
