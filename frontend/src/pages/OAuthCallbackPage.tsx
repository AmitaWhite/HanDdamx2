import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { Icon } from "@/components/ui/Icon";
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

	return (
		<div className="flex min-h-[calc(100vh-72px)] flex-col items-center justify-center gap-4 px-margin-mobile text-center">
			{failed ? (
				<>
					<p className="text-body-md text-on-surface">
						구글 로그인에 실패했습니다.
					</p>
					<Link
						to={paths.login}
						className="text-label-md font-label-md font-bold text-primary hover:underline"
					>
						로그인으로 돌아가기
					</Link>
				</>
			) : (
				<>
					<Icon
						name="progress_activity"
						className="animate-spin text-[32px] text-primary"
					/>
					<p className="text-body-md text-secondary">로그인 처리 중…</p>
				</>
			)}
		</div>
	);
}
