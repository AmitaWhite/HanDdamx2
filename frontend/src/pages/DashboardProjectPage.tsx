import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { getCategories, type CategorySummary } from "@/features/category/categoryApi";
import {
  deleteProject,
  getProject,
  getProjectFeeds,
  updateProject,
} from "@/features/creator/creatorApi";
import type { FeedSummary, FeedVisibility, ProjectSummary, SliceResponse } from "@/features/creator/types";

const VISIBILITY_LABEL: Record<FeedVisibility, string> = {
  PUBLIC: "전체 공개",
  SUBSCRIBERS_ONLY: "구독자 공개",
  PAID_ONLY: "유료",
};

const VISIBILITY_COLOR: Record<FeedVisibility, string> = {
  PUBLIC: "text-secondary",
  SUBSCRIBERS_ONLY: "text-primary",
  PAID_ONLY: "bg-primary/10 text-primary rounded px-1.5 py-0.5 text-[10px] font-bold",
};

function toRelativeLabel(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  if (Number.isNaN(diffMs) || diffMs < 0) return "방금";
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 1) return "방금";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}일 전`;
  return `${Math.floor(days / 7)}주 전`;
}

export function DashboardProjectPage() {
  const { projectId = "" } = useParams();
  const numericId = Number(projectId);
  const navigate = useNavigate();

  const [project, setProject] = useState<ProjectSummary | null>(null);
  const [feedSlice, setFeedSlice] = useState<SliceResponse<FeedSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);

  // 수정 모달
  const [showEdit, setShowEdit] = useState(false);
  const [categories, setCategories] = useState<CategorySummary[]>([]);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editCategoryId, setEditCategoryId] = useState<number | "">("");
  const [editCoverImage, setEditCoverImage] = useState<File | null>(null);
  const [removeCover, setRemoveCover] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  // 삭제
  const [deleting, setDeleting] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    if (!numericId) {
      setError("잘못된 프로젝트입니다.");
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([getProject(numericId), getProjectFeeds(numericId, 0, 20)])
      .then(([proj, feeds]) => {
        if (cancelled) return;
        setProject(proj);
        setFeedSlice(feeds);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "프로젝트 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [numericId]);
  const [loadMoreError, setLoadMoreError] = useState<string | null>(null);

  const handleLoadMore = async () => {
    if (!feedSlice || !feedSlice.hasNext) return;
    setLoadingMore(true);
    setLoadMoreError(null);
    try {
      const next = await getProjectFeeds(numericId, feedSlice.page + 1, feedSlice.size);
      setFeedSlice((prev) =>
        prev ? { ...next, content: [...prev.content, ...next.content] } : next,
      );
    } catch (e) {
      setLoadMoreError(e instanceof Error ? e.message : "게시물을 더 불러오지 못했습니다.");
    } finally {
      setLoadingMore(false);
    }
  };

  const handleOpenEdit = () => {
    if (!project) return;
    setEditTitle(project.title);
    setEditDescription(project.description ?? "");
    setEditCategoryId(project.category.categoryId);
    setEditCoverImage(null);
    setRemoveCover(false);
    setEditError(null);
    getCategories().then(setCategories).catch(() => setCategories([]));
    setShowEdit(true);
  };

  const handleUpdate = async () => {
    if (!project || !editTitle.trim() || editCategoryId === "") return;
    setUpdating(true);
    setEditError(null);
    try {
      const updated = await updateProject(
        project.projectId,
        editTitle.trim(),
        Number(editCategoryId),
        editDescription.trim() || null,
        removeCover,
        editCoverImage,
      );
      setProject(updated);
      setShowEdit(false);
    } catch (e) {
      setEditError(e instanceof Error ? e.message : "수정에 실패했습니다.");
    } finally {
      setUpdating(false);
    }
  };

  const handleDelete = async () => {
    if (!project) return;
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteProject(project.projectId);
      navigate(paths.dashboardProjects);
    } catch (e) {
      setDeleteError(e instanceof Error ? e.message : "삭제에 실패했습니다.");
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return <div className="flex min-h-[60vh] items-center justify-center text-secondary">불러오는 중...</div>;
  }
  if (error || !project) {
    return <div className="flex min-h-[60vh] items-center justify-center text-secondary">{error ?? "프로젝트를 찾을 수 없습니다."}</div>;
  }

  const feeds = feedSlice?.content ?? [];

  return (
    <div className="container-page py-8">
      <Link
        to={paths.dashboardProjects}
        className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
      >
        <Icon name="arrow_back" className="text-[18px]" />
        돌아가기
      </Link>

      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-caption font-caption text-secondary">{project.category.name}</p>
          <h1 className="text-headline-lg font-display text-on-surface">{project.title}</h1>
          {project.description && (
            <p className="mt-1 text-body-md text-secondary">{project.description}</p>
          )}
          <p className="mt-1 text-caption font-caption text-secondary">게시물 {project.feedCount}개</p>
        </div>
        <div className="flex shrink-0 gap-2">
          <Button variant="secondary" size="sm" onClick={handleOpenEdit}>
            수정
          </Button>
          <Button variant="outline" size="sm" onClick={() => setShowDeleteConfirm(true)}>
            삭제
          </Button>
          <LinkButton to={paths.dashboardPostNew} state={{ projectId: project.projectId }}>
            <Icon name="add" className="text-[18px]" />
            새 포스트 작성
          </LinkButton>
        </div>
      </div>

      <div className="flex flex-col gap-4">
        {feeds.map((feed) => (
          <Link key={feed.id} to={paths.postDetail(feed.id)}>
            <Card interactive className="flex gap-4 p-4">
              <div className="min-w-0 flex-1">
                <div className="mb-1 flex items-center gap-2">
                  <span className={VISIBILITY_COLOR[feed.visibility]}>
                    {VISIBILITY_LABEL[feed.visibility]}
                  </span>
                </div>
                <h3 className="truncate text-label-md font-label-md text-on-surface">{feed.title}</h3>
                <p className="mt-1 truncate text-caption font-caption text-secondary">{feed.content}</p>
                <p className="mt-1 text-caption font-caption text-secondary">
                  {toRelativeLabel(feed.createdAt)} · 좋아요 {feed.likeCount} · 댓글 {feed.commentCount}
                </p>
              </div>
            </Card>
          </Link>
        ))}
        {feeds.length === 0 && (
          <p className="py-8 text-center text-body-md text-secondary">아직 게시물이 없어요.</p>
        )}
      </div>

      {feedSlice?.hasNext && (
        <div className="mt-6 text-center">
          {loadMoreError && (
            <p className="mb-2 text-body-sm text-error">{loadMoreError}</p>
          )}
          <Button variant="secondary" onClick={handleLoadMore} disabled={loadingMore}>
            {loadingMore ? "불러오는 중..." : "게시물 더보기"}
          </Button>
        </div>
      )}

      {/* 프로젝트 수정 모달 */}
      {showEdit && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="w-full max-w-md p-6">
            <h2 className="mb-4 text-headline-sm font-display text-on-surface">프로젝트 수정</h2>
            <input
              type="text"
              placeholder="프로젝트 제목 (필수)"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="mb-3 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
            />
            <select
              value={editCategoryId}
              onChange={(e) => setEditCategoryId(e.target.value === "" ? "" : Number(e.target.value))}
              className="mb-3 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/40"
            >
              <option value="">카테고리 선택 (필수)</option>
              {categories.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
              ))}
            </select>
            <textarea
              placeholder="프로젝트 설명 (선택)"
              value={editDescription}
              onChange={(e) => setEditDescription(e.target.value)}
              rows={3}
              className="mb-3 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
            />
            {project.coverImageUrl && (
              <label className="mb-3 flex items-center gap-2 text-body-sm text-on-surface">
                <input
                  type="checkbox"
                  checked={removeCover}
                  onChange={(e) => setRemoveCover(e.target.checked)}
                />
                현재 커버 이미지 삭제
              </label>
            )}
            <input
              type="file"
              accept="image/*"
              className="mb-4 w-full text-body-sm text-on-surface"
              onChange={(e) => {
                const file = e.target.files?.[0] ?? null;
                setEditCoverImage(file);
                if (file) setRemoveCover(false);
              }}
            />
            {editError && <p className="mb-3 text-body-sm text-error">{editError}</p>}
            <div className="flex gap-2">
              <Button
                variant="primary"
                size="sm"
                onClick={handleUpdate}
                disabled={updating || !editTitle.trim() || editCategoryId === ""}
              >
                {updating ? "저장 중..." : "저장"}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setShowEdit(false)}>
                취소
              </Button>
            </div>
          </Card>
        </div>
      )}

      {/* 삭제 확인 모달 */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="w-full max-w-sm p-6">
            <h2 className="mb-2 text-headline-sm font-display text-on-surface">프로젝트 삭제</h2>
            <p className="mb-4 text-body-md text-secondary">
              "{project.title}"을 삭제할까요? 피드가 있으면 삭제할 수 없습니다.
            </p>
            {deleteError && <p className="mb-3 text-body-sm text-error">{deleteError}</p>}
            <div className="flex gap-2">
              <Button variant="primary" size="sm" onClick={handleDelete} disabled={deleting}>
                {deleting ? "삭제 중..." : "삭제"}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setShowDeleteConfirm(false)}>
                취소
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
