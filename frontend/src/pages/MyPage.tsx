import { useEffect, useState } from "react";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import { getMySummary } from "@/features/member/memberApi";
import type { MemberSummaryResponse } from "@/features/member/types";
import { applyCreator, getMyLatestApplication } from "@/features/creator/creatorApplicationApi";
import type { CreatorApplication } from "@/features/creator/types";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "심사 중",
  APPROVED: "승인 완료",
  REJECTED: "거절됨",
};

const STATUS_COLOR: Record<string, string> = {
  PENDING: "text-secondary",
  APPROVED: "text-primary",
  REJECTED: "text-error",
};

export function MyPage() {

	const [summary, setSummary] = useState<MemberSummaryResponse | null> (null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(false);

  const { user } = useAuth();
  const [application, setApplication] = useState<CreatorApplication | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [introduction, setIntroduction] = useState("");
  const [applying, setApplying] = useState(false);
  const [applyError, setApplyError] = useState<string | null>(null);

  const isCreator = user?.role === "CREATOR";
  const isAdmin = user?.role === "ADMIN";

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

	}, [])

  useEffect(() => {
    if (!user || isCreator || isAdmin) return;
    getMyLatestApplication()
      .then(setApplication)
      .catch(() => setApplication(null));
  }, [user, isCreator, isAdmin]);

  const handleApply = async () => {
    setApplyError(null);
    setApplying(true);
    try {
      const result = await applyCreator(introduction);
      setApplication(result);
      setShowModal(false);
      setIntroduction("");
    } catch (e: unknown) {
      setApplyError(e instanceof Error ? e.message : "신청에 실패했습니다.");
    } finally {
      setApplying(false);
    }
  };

	if (loading) {
		return (
      <MyPageShell onCreatorApply={application?.status === "PENDING" ? undefined : () => setShowModal(true)}>
				<p className="py-12 text-center text-body-md text-secondary">불러오는 중...</p>
			</MyPageShell>
		);
	}

	if (error || !summary) {
		return (
      <MyPageShell onCreatorApply={application?.status === "PENDING" ? undefined : () => setShowModal(true)}>
				<p className="py-12 text-center text-body-md text-secondary">프로필 정보를 불러오지 못했습니다.</p>
			</MyPageShell>
		);
	}


	return (
    <MyPageShell onCreatorApply={application?.status === "PENDING" ? undefined : () => setShowModal(true)}>
			<h1 className="mb-6 border-b border-outline-variant pb-4 text-headline-lg font-display text-on-surface">프로필</h1>

			<div className="mb-8 flex flex-col items-center gap-4 rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-8 text-center sm:flex-row sm:text-left">
				<Avatar src={summary.profileImageUrl ?? undefined} size={80} />
				<div className="min-w-0 flex-1">
					<p className="text-display-lg font-display text-on-surface">{summary.nickname}</p>
          {isCreator && <p className="mt-1 text-label-md text-primary">크리에이터</p>}
          {isAdmin && <p className="mt-1 text-label-md text-primary">관리자</p>}
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
      {/* 신청 상태 표시 (USER만) */}
      {!isCreator && !isAdmin && application && (
        <div className="mt-6 rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-6">
          <p className="text-body-md text-on-surface">
            크리에이터 전환 신청 상태:{" "}
            <span className={`font-bold ${STATUS_COLOR[application.status]}`}>
						{STATUS_LABEL[application.status]}
					</span>
          </p>
          {application.status === "REJECTED" && application.rejectReason && (
            <p className="mt-2 text-body-md text-secondary">거절 사유: {application.rejectReason}</p>
          )}
        </div>
      )}

      {/* 관리자 메뉴 */}
      {isAdmin && (
        <div className="mt-6 rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-6">
          <h2 className="mb-3 text-headline-sm font-display text-on-surface">관리자 메뉴</h2>
          <LinkButton to={paths.adminCreatorApplications} variant="secondary" size="sm">
            크리에이터 신청 관리
          </LinkButton>
        </div>
      )}

      {/* 신청 모달 */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="w-full max-w-md p-6">
            <h2 className="mb-4 text-headline-sm font-display text-on-surface">크리에이터 전환 신청</h2>
            <textarea
              className="mb-4 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
              rows={4}
              placeholder="크리에이터 소개글을 입력해 주세요 (선택)"
              value={introduction}
              onChange={(e) => setIntroduction(e.target.value)}
            />
            {applyError && <p className="mb-3 text-body-sm text-error">{applyError}</p>}
            <div className="flex gap-2">
              <Button variant="primary" size="sm" onClick={handleApply} disabled={applying}>
                {applying ? "신청 중..." : "신청하기"}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setShowModal(false)}>
                취소
              </Button>
            </div>
          </Card>
        </div>
      )}
		</MyPageShell>
	);
}
