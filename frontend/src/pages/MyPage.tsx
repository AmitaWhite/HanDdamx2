import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { LinkButton } from "@/components/ui/LinkButton";
import { getMySummary } from "@/features/member/memberApi";
import type { MemberSummaryResponse } from "@/features/member/types";
import { useEffect, useState } from "react";

export function MyPage() {

	const [summary, setSummary] = useState<MemberSummaryResponse | null> (null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(false);

	useEffect(() => {
		let ignore = false;

		const fetchSummary = async () => {
			try {
				const data = await getMySummary();

				if (!ignore) {
					setSummary(data);
				}
			} catch {
				if (!ignore) {
					setError(true);
				}
			} finally {
				if (!ignore) {
					setLoading(false);
				}
			}
		};

		fetchSummary();

		return () => {
			ignore = true;
		};

	}, []);

	if (loading) {
		return (
			<MyPageShell>
				<p className="py-12 text-center text-body-md text-secondary">불러오는 중...</p>
			</MyPageShell>
		);
	}

	if (error || !summary) {
		return (
			<MyPageShell>
				<p className="py-12 text-center text-body-md text-secondary">프로필 정보를 불러오지 못했습니다.</p>
			</MyPageShell>
		);
	}


	return (
		<MyPageShell>
			<h1 className="mb-6 border-b border-outline-variant pb-4 text-headline-lg font-display text-on-surface">프로필</h1>

			<div className="mb-8 flex flex-col items-center gap-4 rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-8 text-center sm:flex-row sm:text-left">
				<Avatar src={summary.profileImageUrl ?? undefined} size={80} />
				<div className="min-w-0 flex-1">
					<p className="text-display-lg font-display text-on-surface">{summary.nickname}</p>
				</div>
				<LinkButton to={paths.mypageSettings} variant="secondary">
					프로필 수정
				</LinkButton>
			</div>

			<div className="grid grid-cols-2 gap-gutter">
				<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-6">
					<p className="text-headline-lg font-display text-primary">{summary.subscribingCount}</p>
					<p className="mt-1 text-body-md text-secondary">구독 중인 크리에이터</p>
				</div>
				<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-6">
					<p className="text-headline-lg font-display text-primary">{summary.myBoardCommentCount}</p>
					<p className="mt-1 text-body-md text-secondary">게시판 댓글</p>
				</div>
			</div>
		</MyPageShell>
	);
}
