import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import {
  approveApplication,
  getAdminApplicationList,
  rejectApplication,
} from "@/features/creator/creatorApplicationApi";
import {
  CREATOR_APPLICATION_STATUS_BG,
  CREATOR_APPLICATION_STATUS_ICON,
  CREATOR_APPLICATION_STATUS_LABEL,
  CREATOR_APPLICATION_STATUS_TEXT_COLOR,
} from "@/features/creator/creatorApplicationStatus";
import type { CreatorApplication, CreatorApplicationStatus, PageResponse } from "@/features/creator/types";

export function AdminCreatorApplicationsPage() {
  const [statusFilter, setStatusFilter] = useState<CreatorApplicationStatus | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [data, setData] = useState<PageResponse<CreatorApplication> | null>(null);
  const [loading, setLoading] = useState(false);
  const [rejectId, setRejectId] = useState<number | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [processing, setProcessing] = useState(false);

  const fetchList = () => {
    setLoading(true);
    getAdminApplicationList(statusFilter, page, 10)
      .then(setData)
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, page]);

  const handleApprove = async (id: number) => {
    setProcessing(true);
    try {
      await approveApplication(id);
      fetchList();
    } catch (e) {
      console.error(e);
    } finally {
      setProcessing(false);
    }
  };

  const handleReject = async () => {
    if (!rejectId || !rejectReason.trim()) return;
    setProcessing(true);
    try {
      await rejectApplication(rejectId, rejectReason.trim());
      setRejectId(null);
      setRejectReason("");
      fetchList();
    } catch (e) {
      console.error(e);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="container-page py-10">
      <h1 className="mb-6 text-headline-lg font-display text-on-surface">크리에이터 신청 관리</h1>

      {/* 상태 필터 */}
      <div className="mb-6 flex gap-2">
        {([undefined, "PENDING", "APPROVED", "REJECTED"] as const).map((s) => (
          <Button
            key={String(s)}
            variant={statusFilter === s ? "primary" : "secondary"}
            size="sm"
            onClick={() => { setStatusFilter(s); setPage(0); }}
          >
            {s === undefined ? "전체" : CREATOR_APPLICATION_STATUS_LABEL[s]}
          </Button>
        ))}
      </div>

      {loading ? (
        <p className="text-secondary">불러오는 중...</p>
      ) : !data || data.content.length === 0 ? (
        <p className="text-secondary">신청 내역이 없습니다.</p>
      ) : (
        <div className="flex flex-col gap-4">
          {data.content.map((app) => (
            <Card key={app.id} className="p-6">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span
                      className={`flex items-center gap-1 rounded-full px-2 py-0.5 text-caption font-caption ${CREATOR_APPLICATION_STATUS_BG[app.status]} ${CREATOR_APPLICATION_STATUS_TEXT_COLOR[app.status]}`}
                    >
                      <Icon name={CREATOR_APPLICATION_STATUS_ICON[app.status]} className="text-[14px]" />
                      {CREATOR_APPLICATION_STATUS_LABEL[app.status]}
                    </span>
                    <span className="text-label-md text-on-surface">신청 ID: {app.id}</span>
                    <span className="text-caption text-secondary">회원 ID: {app.memberId}</span>
                  </div>
                  {app.introduction && (
                    <p className="mt-2 text-body-md text-on-surface">{app.introduction}</p>
                  )}
                  {app.rejectReason && (
                    <p className="mt-1 text-body-md text-secondary">거절 사유: {app.rejectReason}</p>
                  )}
                  <p className="mt-2 text-caption text-secondary">
                    신청일: {new Date(app.appliedAt).toLocaleString("ko-KR")}
                  </p>
                </div>
                {app.status === "PENDING" && (
                  <div className="flex shrink-0 gap-2">
                    <Button
                      variant="primary"
                      size="sm"
                      disabled={processing}
                      onClick={() => handleApprove(app.id)}
                    >
                      승인
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={processing}
                      onClick={() => { setRejectId(app.id); setRejectReason(""); }}
                    >
                      거절
                    </Button>
                  </div>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* 페이지네이션 */}
      {data && data.totalPages > 1 && (
        <div className="mt-6 flex items-center justify-center gap-2">
          <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>
            이전
          </Button>
          <span className="text-body-md text-secondary">{page + 1} / {data.totalPages}</span>
          <Button variant="secondary" size="sm" disabled={data.last} onClick={() => setPage(page + 1)}>
            다음
          </Button>
        </div>
      )}

      {/* 거절 사유 모달 */}
      {rejectId !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="w-full max-w-md p-6">
            <h2 className="mb-4 text-headline-sm font-display text-on-surface">거절 사유 입력</h2>
            <textarea
              className="mb-4 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
              rows={4}
              placeholder="거절 사유를 입력해 주세요 (필수)"
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
            />
            <div className="flex gap-2">
              <Button
                variant="primary"
                size="sm"
                disabled={processing || !rejectReason.trim()}
                onClick={handleReject}
              >
                {processing ? "처리 중..." : "거절 확정"}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setRejectId(null)}>
                취소
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
