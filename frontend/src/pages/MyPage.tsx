import { useEffect, useState } from "react";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { CreatorApplicationStatus } from "@/components/creator/CreatorApplicationStatus";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import { refreshAccessToken } from "@/lib/api";
import { getMySummary } from "@/features/member/memberApi";
import type { MemberSummaryResponse } from "@/features/member/types";
import { applyCreator, getMyLatestApplication } from "@/features/creator/creatorApplicationApi";
import type { CreatorApplication } from "@/features/creator/types";

export function MyPage() {

	const [summary, setSummary] = useState<MemberSummaryResponse | null> (null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(false);

  const { user } = useAuth();
  const [application, setApplication] = useState<CreatorApplication | null>(null);
  const [applicationLoading, setApplicationLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [introduction, setIntroduction] = useState("");
  const [applying, setApplying] = useState(false);
  const [representativeImage, setRepresentativeImage] = useState<File | null>(null);
  const [applyError, setApplyError] = useState<string | null>(null);

  const isCreator = user?.role === "CREATOR";
  const isAdmin = user?.role === "ADMIN";

  const roleBadge = isAdmin
    ? { label: "관리자", className: "bg-tertiary text-on-tertiary" }
    : isCreator
    ? { label: "크리에이터", className: "bg-primary text-on-primary" }
    : application?.status === "PENDING"
    ? { label: "심사 중", className: "bg-yellow-100 text-yellow-800" }
    : { label: "일반 사용자", className: "bg-surface-container text-secondary" };

  const onCreatorApply =
    application?.status === "PENDING" ? undefined : () => setShowModal(true);

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

	// user.role이 바뀌면(예: 크리에이터 승인 반영) 자기소개 등 role 종속 데이터를 다시 받아온다.
	}, [user?.role])

  useEffect(() => {
    if (!user || user.role !== "USER") {
      setApplication(null);
      setApplicationLoading(false);
      return;
    }
    let ignore = false;
    setApplicationLoading(true);
    getMyLatestApplication()
      .then((data) => {
        if (ignore) return;
        setApplication(data);
        // 신청은 승인됐는데 토큰의 role이 아직 USER면, 토큰을 갱신해 role 동기화
        if (data?.status === "APPROVED") {
          refreshAccessToken().catch(() => {});
        }
      })
      .catch(() => {
        if (!ignore) setApplication(null);
      })
      .finally(() => {
        if (!ignore) setApplicationLoading(false);
      });
    return () => {
      ignore = true;
    };
  }, [user]);

  const handleApply = async () => {
    const trimmedIntroduction = introduction.trim();
    setApplyError(null);
    setApplying(true);
    try {
      const result = await applyCreator(trimmedIntroduction, representativeImage);
      setApplication(result);
      setShowModal(false);
      setIntroduction("");
      setRepresentativeImage(null);
    } catch (e: unknown) {
      setApplyError(e instanceof Error ? e.message : "신청에 실패했습니다.");
    } finally {
      setApplying(false);
    }
  };

	if (loading || applicationLoading) {
		return (
      <MyPageShell onCreatorApply={onCreatorApply}>
				<p className="py-12 text-center text-body-md text-secondary">불러오는 중...</p>
			</MyPageShell>
		);
	}

	if (error || !summary) {
		return (
      <MyPageShell onCreatorApply={onCreatorApply}>
				<p className="py-12 text-center text-body-md text-secondary">프로필 정보를 불러오지 못했습니다.</p>
			</MyPageShell>
		);
	}


	return (
    <MyPageShell onCreatorApply={onCreatorApply}>
			<h1 className="mb-6 border-b border-outline-variant pb-4 text-headline-lg font-display text-on-surface">프로필</h1>

			<div className="relative mb-8 flex flex-col items-center gap-4 rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-8 pt-14 text-center">
        <span
          className={`absolute left-6 top-6 rounded-full px-3 py-1 text-caption font-caption font-bold ${roleBadge.className}`}
        >
          {roleBadge.label}
        </span>

				<Avatar
					src={summary.profileImageUrl ?? undefined}
					fallbackText={summary.nickname}
					size={140}
				/>
				<div className="min-w-0">
					<p className="text-[29px] font-display font-semibold text-on-surface">{summary.nickname}</p>
				</div>
				<LinkButton to={paths.mypageSettings} variant="secondary" className="mt-2">
					프로필 수정
				</LinkButton>
        {isCreator && summary.introduction && (
          <p className="mt-4 max-w-md text-body-md text-on-surface">{summary.introduction}</p>
        )}
        {!isCreator && !isAdmin && <CreatorApplicationStatus application={application} />}
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
            <input
              type="file"
              accept="image/*"
              className="mb-4 w-full text-body-sm text-on-surface"
              onChange={(e) => setRepresentativeImage(e.target.files?.[0] ?? null)}
            />
            {applyError && <p className="mb-3 text-body-md text-error">{applyError}</p>}
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
