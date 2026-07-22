import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { useSubmitState } from "@/features/auth/useSubmitState";
import {
	addFeedAttachment,
	deleteFeed,
	deleteFeedAttachment,
	getFeed,
	getFeedAttachments,
	moveFeedProject,
	updateFeed,
} from "@/features/feed/feedApi";
import type { AttachmentResponse, Visibility } from "@/features/feed/types";
import { getMyProjects } from "@/features/project/projectApi";
import type { ProjectResponse } from "@/features/project/types";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { applyFormat, insertLink, prefixCurrentLine, wrapSelection } from "@/lib/textareaFormatting";

const VISIBILITY_OPTIONS: { id: Visibility; label: string; description: string }[] = [
	{ id: "PUBLIC", label: "전체공개", description: "누구나 볼 수 있어요" },
	{ id: "FREE_SUBSCRIBER", label: "무료 구독자만", description: "무료 이상 구독자에게 공개" },
	{ id: "PAID_SUBSCRIBER", label: "유료 구독자만", description: "유료 구독자에게만 공개" },
];

// 직접 재생 가능한 동영상 파일 URL인지 확인 — VIDEO_LINK엔 유튜브 시청 페이지 같은
// <video>로 재생 불가능한 링크도 들어올 수 있어(예: 시나리오 시드 데이터), mimeType이나
// 확장자로 실제 재생 가능한 파일인지 먼저 확인한다.
const PLAYABLE_VIDEO_EXTENSION = /\.(mp4|webm|ogg|ogv|mov|m4v)(\?.*)?$/i;

function isPlayableVideo(attachment: AttachmentResponse): boolean {
	if (attachment.mimeType?.startsWith("video/")) return true;
	return attachment.type === "VIDEO_LINK" && PLAYABLE_VIDEO_EXTENSION.test(attachment.url);
}

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
	const [confirmAttachmentId, setConfirmAttachmentId] = useState<number | null>(null);
	const [deletingAttachmentIds, setDeletingAttachmentIds] = useState<Set<number>>(new Set());
	const [confirmDeletePost, setConfirmDeletePost] = useState(false);
	const [deleteError, setDeleteError] = useState<string | null>(null);

	const [attachments, setAttachments] = useState<AttachmentResponse[]>([]);
	const [attachmentError, setAttachmentError] = useState<string | null>(null);
	const [uploading, setUploading] = useState(false);
	const fileInputRef = useRef<HTMLInputElement>(null);
	const contentRef = useRef<HTMLTextAreaElement>(null);

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

	useEffect(() => {
		let cancelled = false;
		setAttachments([]);
		setAttachmentError(null);
		getFeedAttachments(feedId)
			.then((list) => {
				if (cancelled) return;
				setAttachments(list);
			})
			.catch((err) => {
				if (cancelled) return;
				setAttachmentError(err instanceof ApiError ? err.message : "첨부파일을 불러오지 못했습니다.");
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

	async function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
		const files = e.target.files;
		if (!files || files.length === 0) return;
		const selectedFiles = Array.from(files);
		e.target.value = "";
		setAttachmentError(null);
		setUploading(true);
		try {
			for (const file of selectedFiles) {
				const uploaded = await addFeedAttachment(Number(feedId), file);
				setAttachments((prev) => [...prev, uploaded]);
			}
		} catch (err) {
			setAttachmentError(err instanceof ApiError ? err.message : "첨부파일 업로드에 실패했습니다.");
		} finally {
			setUploading(false);
		}
	}

	function onDeleteAttachment(attachmentId: number) {
		if (deletingAttachmentIds.has(attachmentId)) return;
		setConfirmAttachmentId(attachmentId);
	}

	async function executeDeleteAttachment() {
		const attachmentId = confirmAttachmentId;
		setConfirmAttachmentId(null);
		if (attachmentId == null) return;
		setAttachmentError(null);
		setDeletingAttachmentIds((prev) => new Set(prev).add(attachmentId));
		try {
			await deleteFeedAttachment(feedId, attachmentId);
			setAttachments((prev) => prev.filter((a) => a.id !== attachmentId));
		} catch (err) {
			setAttachmentError(err instanceof ApiError ? err.message : "첨부파일 삭제에 실패했습니다.");
		} finally {
			setDeletingAttachmentIds((prev) => {
				const next = new Set(prev);
				next.delete(attachmentId);
				return next;
			});
		}
	}

	function onBold() {
		const el = contentRef.current;
		if (!el) return;
		applyFormat(el, setContent, wrapSelection(el.value, el.selectionStart, el.selectionEnd, "**", "**", "굵게 강조"));
	}
	function onItalic() {
		const el = contentRef.current;
		if (!el) return;
		applyFormat(el, setContent, wrapSelection(el.value, el.selectionStart, el.selectionEnd, "*", "*", "기울임"));
	}
	function onList() {
		const el = contentRef.current;
		if (!el) return;
		applyFormat(el, setContent, prefixCurrentLine(el.value, el.selectionStart, el.selectionEnd, "- "));
	}
	function onLink() {
		const el = contentRef.current;
		if (!el) return;
		const url = window.prompt("링크 URL을 입력하세요 (https://...)");
		if (!url) return;
		applyFormat(el, setContent, insertLink(el.value, el.selectionStart, el.selectionEnd, url));
	}

	const toolbarButtons = [
		{ icon: "format_bold", title: "굵게", onClick: onBold },
		{ icon: "format_italic", title: "기울임", onClick: onItalic },
		{ icon: "format_list_bulleted", title: "목록", onClick: onList },
		{ icon: "image", title: "이미지·동영상·PDF 추가", onClick: () => fileInputRef.current?.click() },
		{ icon: "link", title: "링크 추가", onClick: onLink },
	];

	async function onMoveProject() {
		if (!projectId || Number(projectId) === currentProjectId) return;
		const result = await runMove(() => moveFeedProject(feedId, Number(projectId)));
		if (result) setCurrentProjectId(Number(projectId));
	}

	function onDelete() {
		setConfirmDeletePost(true);
	}

	async function executeDeletePost() {
		setConfirmDeletePost(false);
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
		<>
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
					{attachmentError && <Alert>{attachmentError}</Alert>}
					<div className="rounded border border-outline-variant bg-surface-container-lowest">
						<div className="flex items-center gap-1 rounded-t border-b border-outline-variant bg-surface-container-low p-2">
							{toolbarButtons.map((btn) => (
								<button
									key={btn.icon}
									type="button"
									onClick={btn.onClick}
									disabled={btn.icon === "image" && uploading}
									title={btn.title}
									aria-label={btn.title}
									className="flex h-8 w-8 items-center justify-center rounded text-secondary hover:bg-surface-container disabled:opacity-50"
								>
									<Icon name={btn.icon} className="text-[18px]" />
								</button>
							))}
							{uploading && <span className="text-caption font-caption text-secondary">업로드 중…</span>}
							<input
								ref={fileInputRef}
								type="file"
								multiple
								className="hidden"
								onChange={onFilesSelected}
								accept="image/*,video/*,application/pdf"
							/>
						</div>
						<textarea
							ref={contentRef}
							id="edit-content"
							value={content}
							onChange={(e) => setContent(e.target.value)}
							rows={10}
							className="w-full resize-y border-0 bg-transparent p-4 text-body-md focus:outline-none focus:ring-0"
						/>
						{attachments.length > 0 && (
							<div className="flex flex-wrap gap-3 rounded-b border-t border-outline-variant p-3">
								{attachments.map((a) => {
									const isVideo = isPlayableVideo(a);
									return (
										<div key={a.id} className="relative h-20 w-20 overflow-hidden rounded-lg">
											{a.type === "IMAGE" ? (
												<img src={a.url} alt="첨부 미리보기" className="h-full w-full object-cover" />
											) : isVideo ? (
												<div className="relative h-full w-full bg-black">
													<video src={a.url} className="h-full w-full object-cover" muted />
													<div className="absolute inset-0 flex items-center justify-center bg-black/20">
														<Icon name="play_circle" className="text-[28px] text-white" />
													</div>
												</div>
											) : (
												<div className="flex h-full w-full flex-col items-center justify-center gap-1 bg-surface-container-low p-1.5 text-center">
													<Icon name="description" className="text-[24px] text-secondary" />
													<span className="line-clamp-2 break-all text-[10px] leading-tight text-secondary">
														{a.originalName ?? "첨부파일"}
													</span>
												</div>
											)}
											<button
												type="button"
												onClick={() => onDeleteAttachment(a.id)}
												disabled={deletingAttachmentIds.has(a.id)}
												aria-label="첨부 삭제"
												className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-inverse-surface/80 text-inverse-on-surface disabled:opacity-50"
											>
												<Icon name="close" className="text-[14px]" />
											</button>
										</div>
									);
								})}
							</div>
						)}
					</div>
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
		<ConfirmDialog
			open={confirmAttachmentId != null}
			title="이 첨부파일을 삭제하시겠어요?"
			confirmLabel="삭제"
			onConfirm={executeDeleteAttachment}
			onCancel={() => setConfirmAttachmentId(null)}
		/>
		<ConfirmDialog
			open={confirmDeletePost}
			title="이 게시물을 삭제하시겠어요?"
			description="삭제하면 되돌릴 수 없습니다."
			confirmLabel="삭제"
			onConfirm={executeDeletePost}
			onCancel={() => setConfirmDeletePost(false)}
		/>
		</>
	);
}
