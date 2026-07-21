import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { getCategories, type CategorySummary } from "@/features/category/categoryApi";
import { createProject, getMyProjects } from "@/features/creator/creatorApi";
import type { ProjectSummary } from "@/features/creator/types";

export function DashboardProjectsPage() {
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [showModal, setShowModal] = useState(false);
  const [categories, setCategories] = useState<CategorySummary[]>([]);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [coverImage, setCoverImage] = useState<File | null>(null);
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getMyProjects()
      .then((data) => {
        if (!cancelled) setProjects(data);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e.message : "프로젝트 목록을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleOpenModal = () => {
    setTitle("");
    setDescription("");
    setCategoryId("");
    setCoverImage(null);
    setCreateError(null);
    getCategories().then(setCategories).catch(() => setCategories([]));
    setShowModal(true);
  };

  const handleCreate = async () => {
    if (!title.trim() || categoryId === "") return;
    setCreating(true);
    setCreateError(null);
    try {
      const newProject = await createProject(title.trim(), Number(categoryId), description.trim() || null, coverImage);
      setProjects((prev) => [newProject, ...prev]);
      setShowModal(false);
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : "프로젝트 생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  return (
    <MyPageShell>
      <div className="relative">
        <div className="mb-8">
          <h1 className="text-headline-lg font-display text-on-surface">프로젝트</h1>
          <p className="mt-1 text-body-md text-secondary">진행 중인 작업을 프로젝트로 묶어 기록해보세요.</p>
        </div>

        {loading ? (
          <p className="py-12 text-center text-body-md text-secondary">불러오는 중...</p>
        ) : error ? (
          <p className="py-12 text-center text-body-md text-secondary">{error}</p>
        ) : projects.length === 0 ? (
          <p className="py-12 text-center text-body-md text-secondary">아직 프로젝트가 없어요. 첫 프로젝트를 만들어보세요!</p>
        ) : (
          <div className="grid grid-cols-1 gap-gutter sm:grid-cols-2">
            {projects.map((project) => (
              <Link key={project.projectId} to={paths.dashboardProject(project.projectId)}>
                <Card interactive className="overflow-hidden">
                  <div className="aspect-[4/3] overflow-hidden bg-surface-container-low">
                    {project.coverImageUrl ? (
                      <img
                        src={project.coverImageUrl}
                        alt={project.title}
                        className="h-full w-full object-cover"
                      />
                    ) : (
                      <div className="flex h-full w-full items-center justify-center">
                        <Icon name="folder" className="text-[48px] text-outline-variant" />
                      </div>
                    )}
                  </div>
                  <div className="p-5">
                    <div className="mb-1 flex items-center justify-between gap-2">
                      <h3 className="truncate text-headline-md font-display text-on-surface">{project.title}</h3>
                      <span className="shrink-0 text-caption font-caption text-secondary">{project.category.name}</span>
                    </div>
                    <p className="text-caption font-caption text-secondary">게시물 {project.feedCount}개</p>
                  </div>
                </Card>
              </Link>
            ))}
          </div>
        )}

        <button
          type="button"
          aria-label="새 프로젝트 만들기"
          onClick={handleOpenModal}
          className="fixed bottom-8 right-8 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-on-primary shadow-card-hover transition-colors hover:bg-primary-hover"
        >
          <Icon name="add" className="text-[28px]" />
        </button>
      </div>

      {/* 프로젝트 생성 모달 */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="w-full max-w-md p-6">
            <h2 className="mb-4 text-headline-sm font-display text-on-surface">새 프로젝트 만들기</h2>
            <input
              type="text"
              placeholder="프로젝트 제목 (필수)"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="mb-3 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
            />
            <select
              value={categoryId}
              onChange={(e) => setCategoryId(e.target.value === "" ? "" : Number(e.target.value))}
              className="mb-3 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/40"
            >
              <option value="">카테고리 선택 (필수)</option>
              {categories.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>{c.name}</option>
              ))}
            </select>
            <textarea
              placeholder="프로젝트 설명 (선택)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="mb-3 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
            />
            <input
              type="file"
              accept="image/*"
              className="mb-4 w-full text-body-sm text-on-surface"
              onChange={(e) => setCoverImage(e.target.files?.[0] ?? null)}
            />
            {createError && <p className="mb-3 text-body-sm text-error">{createError}</p>}
            <div className="flex gap-2">
              <Button
                variant="primary"
                size="sm"
                onClick={handleCreate}
                disabled={creating || !title.trim() || categoryId === ""}
              >
                {creating ? "생성 중..." : "만들기"}
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
