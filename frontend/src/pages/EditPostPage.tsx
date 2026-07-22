import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { useSubmitState } from "@/features/auth/useSubmitState";
import { deleteFeed, getFeed, moveFeedProject, updateFeed } from "@/features/feed/feedApi";
import type { Visibility } from "@/features/feed/types";
import { getMyProjects } from "@/features/project/projectApi";
import type { ProjectResponse } from "@/features/project/types";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";

const VISIBILITY_OPTIONS: { id: Visibility; label: string; description: string }[] = [
	{ id: "PUBLIC", label: "전체공개", description: "누구나 볼 수 있어요" },
	{ id: "FREE_SUBSCRIBER", label: "무료 구독자만", description: "무료 이상 구독자에게 공개" },
	{ id: "PAID_SUBSCRIBER", label: "유료 구독자만", description: "유료 구독자에게만 공개" },
];

export function EditPostPage() {
	const { feedId = "" } = useParams();
	const navigate = useNavigate();

	const [loading, setLoading] = useState(true);
	const [loadError, setLoadError] = useState<string | null>(null);

	const [title, setTitle] = useState("");
	const [content, setContent] = useState("");
	const [visibility, setVisibility] = useState<Visibility>("FREE_SUBSCRIBER");
	const [fieldError, setFieldError] = useState<string | undefined>();

	const [projects, setProjects] = useState<ProjectResponse[]>([]);
	const [projectId, setProjectId] = useState("");
	const [currentProjectId, setCurrentProjectId] = useState<number | null>(null);

	const { loading: saving, error: saveError, run: runSave } = useSubmitState(
		"수정에 실패했습니다. 다시 시도해 주세요.",
	);
	const { loading: moving, error: moveError, run: runMove } = useSubmitState(
		"프로젝트 이동에 실패했습니다. 다시 시도해 주세요.",
	);
	const [deleting, setDeleting] = useState(false);
	const [deleteError, setDeleteError] = useState<string | null>(null);

	useEffect(() => {
		let cancelled = false;
		setLoading(true);
		setLoadError(null);
		Promise.all([getFeed(feedId), getMyProjects()])
			.then(([feed, projectList]) => {
				if (cancelled) return;
				setTitle(feed.title);
				setContent(feed.content ?? "");
				setVisibility(feed.visibility);
				setProjects(projectList);
				setProjectId(String(feed.projectId));
				setCurrentProjectId(feed.projectId);
			})
			.catch((err) => {
				if (cancelled) return;
				setLoadError(err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.");
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});
		return () => {
			cancelled = true;
		};
	}, [feedId]);

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		const trimmedTitle = title.trim();
		const trimmedContent = content.trim();
		if (!trimmedTitle || !trimmedContent) {
			setFieldError("제목과 본문을 입력해 주세요.");
			return;
		}
		setFieldError(undefined);

		const result = await runSave(() =>
			updateFeed(feedId, { title: trimmedTitle, content: trimmedContent, visibility }),
		);
		if (result) navigate(paths.postDetail(feedId));
	}

	async function onMoveProject() {
		if (!projectId || Number(projectId) === currentProjectId) return;
		const result = await runMove(() => moveFeedProject(feedId, Number(projectId)));
		if (result) setCurrentProjectId(Number(projectId));
	}

	async function onDelete() {
		if (!window.confirm("이 게시물을 삭제하시겠어요? 삭제하면 되돌릴 수 없습니다.")) return;
		setDeleteError(null);
		setDeleting(true);
		try {
			await deleteFeed(feedId);
			navigate(paths.dashboardPosts);
		} catch (err) {
			setDeleteError(err instanceof ApiError ? err.message : "삭제에 실패했습니다.");
			setDeleting(false);
		}
	}

	if (loading) {
		return <p className="container-page py-16 text-center text-body-md text-secondary">불러오는 중…</p>;
	}
	if (loadError) {
		return <p className="container-page py-16 text-center text-body-md text-secondary">{loadError}</p>;
	}

	return (
		<div className="container-page max-w-2xl py-8">
			<h1 className="mb-6 text-headline-lg font-display text-on-surface">게시물 수정</h1>

			<form onSubmit={onSubmit} noValidate className="flex flex-col gap-5">
				{saveError && <Alert>{saveError}</Alert>}
				{fieldError && <Alert>{fieldError}</Alert>}

				<Input label="제목" value={title} onChange={(e) => setTitle(e.target.value)} maxLength={255} />

				<div className="flex flex-col gap-1.5">
					<label className="text-label-md font-label-md text-on-surface" htmlFor="edit-content">
						본문
					</label>
					<textarea
						id="edit-content"
						value={content}
						onChange={(e) => setContent(e.target.value)}
						rows={10}
						className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest p-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					/>
				</div>

				<div>
					<p className="mb-3 text-label-md font-label-md text-on-surface">공개 범위</p>
					<div className="flex flex-col gap-2">
						{VISIBILITY_OPTIONS.map((opt) => (
							<button
								key={opt.id}
								type="button"
								onClick={() => setVisibility(opt.id)}
								className={cn(
									"rounded-lg border p-3 text-left transition-colors",
									visibility === opt.id ? "border-primary bg-primary/5" : "border-outline-variant",
								)}
							>
								<p className="text-label-md font-label-md text-on-surface">{opt.label}</p>
								<p className="text-caption font-caption text-secondary">{opt.description}</p>
							</button>
						))}
					</div>
				</div>

				<div className="flex gap-3">
					<Button
						type="button"
						variant="secondary"
						fullWidth
						disabled={saving}
						onClick={() => navigate(paths.postDetail(feedId))}
					>
						취소
					</Button>
					<Button type="submit" fullWidth disabled={saving}>
						{saving ? "저장하는 중…" : "저장하기"}
					</Button>
				</div>
			</form>

			<div className="mt-8 rounded-lg border border-outline-variant p-4">
				<p className="mb-3 text-label-md font-label-md text-on-surface">프로젝트 이동</p>
				{moveError && <Alert className="mb-3">{moveError}</Alert>}
				<div className="flex gap-2">
					<select
						value={projectId}
						onChange={(e) => setProjectId(e.target.value)}
						className="h-11 flex-1 rounded border border-outline-variant bg-surface-container-lowest px-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					>
						{projects.map((p) => (
							<option key={p.projectId} value={p.projectId}>
								{p.title}
							</option>
						))}
					</select>
					<Button
						type="button"
						variant="secondary"
						onClick={onMoveProject}
						disabled={moving || Number(projectId) === currentProjectId}
					>
						{moving ? "이동 중…" : "이동"}
					</Button>
				</div>
			</div>

			<div className="mt-8 rounded-lg border border-error/40 p-4">
				<p className="mb-2 text-label-md font-label-md text-error">삭제</p>
				<p className="mb-3 text-caption font-caption text-secondary">삭제하면 되돌릴 수 없습니다.</p>
				{deleteError && <Alert className="mb-3">{deleteError}</Alert>}
				<Button type="button" variant="secondary" onClick={onDelete} disabled={deleting} className="text-error">
					{deleting ? "삭제하는 중…" : "게시물 삭제"}
				</Button>
			</div>
		</div>
	);
}
