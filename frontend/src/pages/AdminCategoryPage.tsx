import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import type { CategoryResponse } from "@/features/category/types";
import {
  createCategory,
  getAllCategories,
  updateActivation,
  updateCategoryName,
} from "@/features/category/categoryApi";

export function AdminCategoryPage() {
  const [categories, setCategories] = useState<CategoryResponse[]>([]);
  const [loading, setLoading] = useState(false);

  // 생성
  const [newName, setNewName] = useState("");
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // 이름 수정 모달
  const [editTarget, setEditTarget] = useState<CategoryResponse | null>(null);  const [editName, setEditName] = useState("");
  const [updating, setUpdating] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const fetchList = () => {
    setLoading(true);
    getAllCategories()
      .then(setCategories)
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    fetchList();
  }, []);

  const handleCreate = async () => {
    if (!newName.trim()) return;
    setCreating(true);
    setCreateError(null);
    try {
      await createCategory(newName.trim());
      setNewName("");
      fetchList();
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : "생성에 실패했습니다.");
    } finally {
      setCreating(false);
    }
  };

  const handleOpenEdit = (category: CategoryResponse) => {
    setEditTarget(category);
    setEditName(category.name);
    setEditError(null);
  };

  const handleUpdate = async () => {
    if (!editTarget || !editName.trim()) return;
    setUpdating(true);
    setEditError(null);
    try {
      await updateCategoryName(editTarget.categoryId, editName.trim());
      setEditTarget(null);
      fetchList();
    } catch (e) {
      setEditError(e instanceof Error ? e.message : "수정에 실패했습니다.");
    } finally {
      setUpdating(false);
    }
  };

  const handleToggleActivation = async (category: CategoryResponse) => {
    try {
      await updateActivation(category.categoryId, !category.active);
      fetchList();
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="container-page py-10">
      <h1 className="mb-6 text-headline-lg font-display text-on-surface">카테고리 관리</h1>

      {/* 카테고리 생성 */}
      <Card className="mb-6 p-6">
        <h2 className="mb-3 text-headline-sm font-display text-on-surface">새 카테고리 추가</h2>
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="카테고리 이름"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") handleCreate(); }}
            className="flex-1 rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
          />
          <Button
            variant="primary"
            size="sm"
            onClick={handleCreate}
            disabled={creating || !newName.trim()}
          >
            {creating ? "추가 중..." : "추가"}
          </Button>
        </div>
        {createError && <p className="mt-2 text-body-sm text-error">{createError}</p>}
      </Card>

      {/* 카테고리 목록 */}
      {loading ? (
        <p className="text-secondary">불러오는 중...</p>
      ) : categories.length === 0 ? (
        <p className="text-secondary">카테고리가 없습니다.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {categories.map((cat) => (
            <Card key={cat.categoryId} className="flex items-center justify-between gap-4 p-4">
              <div className="flex items-center gap-3">
                <span
                  className={`rounded-full px-2 py-0.5 text-caption font-caption ${
                    cat.active
                      ? "bg-primary/10 text-primary"
                      : "bg-surface-container text-secondary"
                  }`}
                >
                  {cat.active ? "활성" : "비활성"}
                </span>
                <span className="text-body-md text-on-surface">{cat.name}</span>
              </div>
              <div className="flex shrink-0 gap-2">
                <Button variant="secondary" size="sm" onClick={() => handleOpenEdit(cat)}>
                  이름 수정
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleToggleActivation(cat)}
                >
                  {cat.active ? "비활성화" : "활성화"}
                </Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* 이름 수정 모달 */}
      {editTarget !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
          <Card className="w-full max-w-md p-6">
            <h2 className="mb-4 text-headline-sm font-display text-on-surface">카테고리 이름 수정</h2>
            <input
              type="text"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              onKeyDown={(e) => { if (e.key === "Enter") handleUpdate(); }}
              className="mb-4 w-full rounded-lg border border-outline-variant bg-surface-container-low p-3 text-body-md text-on-surface placeholder:text-secondary focus:outline-none focus:ring-2 focus:ring-primary/40"
            />
            {editError && <p className="mb-3 text-body-sm text-error">{editError}</p>}
            <div className="flex gap-2">
              <Button
                variant="primary"
                size="sm"
                onClick={handleUpdate}
                disabled={updating || !editName.trim()}
              >
                {updating ? "저장 중..." : "저장"}
              </Button>
              <Button variant="secondary" size="sm" onClick={() => setEditTarget(null)}>
                취소
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
