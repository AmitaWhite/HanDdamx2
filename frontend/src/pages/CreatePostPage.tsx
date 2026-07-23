import { useEffect, useRef, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { endOfDayToIso } from "@/lib/date";
import { applyFormat, insertLink, prefixCurrentLine, wrapSelection } from "@/lib/textareaFormatting";
import { useSubmitState } from "@/features/auth/useSubmitState";
import { addFeedAttachment, createFeed, deleteFeed } from "@/features/feed/feedApi";
import type { Visibility } from "@/features/feed/types";
import { createFeedPoll } from "@/features/poll/pollApi";
import { getMyProjects } from "@/features/project/projectApi";
import type { ProjectResponse } from "@/features/project/types";

interface CreatePostLocationState {
	projectId?: string | number;
}

interface AttachmentItem {
	id: string;
	file: File;
	previewUrl: string;
}

const VISIBILITY_OPTIONS: { id: Visibility; label: string; description: string }[] = [
	{ id: "PUBLIC", label: "전체공개", description: "누구나 볼 수 있어요" },
	{ id: "FREE_SUBSCRIBER", label: "무료 구독자만", description: "무료 이상 구독자에게 공개" },
	{ id: "PAID_SUBSCRIBER", label: "유료 구독자만", description: "유료 구독자에게만 공개" },
];

export function CreatePostPage() {
	const navigate = useNavigate();
	const location = useLocation();
	const locationState = (location.state ?? {}) as CreatePostLocationState;

	const [projects, setProjects] = useState<ProjectResponse[]>([]);
	const [projectsLoading, setProjectsLoading] = useState(true);
	const [projectsError, setProjectsError] = useState<string | null>(null);

	const [projectId, setProjectId] = useState("");
	const [title, setTitle] = useState("");
	const [body, setBody] = useState("");
	const [attachments, setAttachments] = useState<AttachmentItem[]>([]);
	const [visibility, setVisibility] = useState<Visibility>("FREE_SUBSCRIBER");
	const [pollEnabled, setPollEnabled] = useState(false);
	const [pollQuestion, setPollQuestion] = useState("");
	const [pollOptions, setPollOptions] = useState(["", ""]);
	const [pollEndAt, setPollEndAt] = useState("");
	const [fieldError, setFieldError] = useState<string | undefined>();

	const attachmentsRef = useRef<AttachmentItem[]>([]);
	attachmentsRef.current = attachments;
	const fileInputRef = useRef<HTMLInputElement>(null);
	const bodyRef = useRef<HTMLTextAreaElement>(null);

	const {
		loading,
		error: serverError,
		run,
	} = useSubmitState("게시물 작성에 실패했습니다. 다시 시도해 주세요.");

	// 내 프로젝트 목록 로드 — 피드는 반드시 프로젝트에 속해야 한다(FeedCreateRequest.projectId 필수).
	useEffect(() => {
		getMyProjects()
			.then((list) => {
				setProjects(list);
				const preferredId =
					locationState.projectId != null ? Number(locationState.projectId) : undefined;
				const initial = list.find((p) => p.projectId === preferredId) ?? list[0];
				if (initial) setProjectId(String(initial.projectId));
			})
			.catch((err) => {
				setProjectsError(
					err instanceof ApiError ? err.message : "프로젝트 목록을 불러오지 못했습니다.",
				);
			})
			.finally(() => setProjectsLoading(false));
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, []);

	// 언마운트 시에만 첨부 미리보기 Blob URL을 정리 (StrictMode useEffect revoke 레이스 방지)
	useEffect(() => {
		return () => {
			for (const a of attachmentsRef.current) {
				URL.revokeObjectURL(a.previewUrl);
			}
		};
	}, []);

	function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
		const files = e.target.files;
		if (!files) return;
		const next = Array.from(files).map((file) => ({
			id: `${file.name}-${file.size}-${Date.now()}`,
			file,
			previewUrl: URL.createObjectURL(file),
		}));
		setAttachments((prev) => [...prev, ...next]);
		e.target.value = "";
	}

	function removeAttachment(id: string) {
		setAttachments((prev) => {
			const target = prev.find((a) => a.id === id);
			if (target) URL.revokeObjectURL(target.previewUrl);
			return prev.filter((a) => a.id !== id);
		});
	}

	function onBold() {
		const el = bodyRef.current;
		if (!el) return;
		applyFormat(el, setBody, wrapSelection(el.value, el.selectionStart, el.selectionEnd, "**", "**", "굵게 강조"));
	}
	function onItalic() {
		const el = bodyRef.current;
		if (!el) return;
		applyFormat(el, setBody, wrapSelection(el.value, el.selectionStart, el.selectionEnd, "*", "*", "기울임"));
	}
	function onList() {
		const el = bodyRef.current;
		if (!el) return;
		applyFormat(el, setBody, prefixCurrentLine(el.value, el.selectionStart, el.selectionEnd, "- "));
	}
	function onLink() {
		const el = bodyRef.current;
		if (!el) return;
		const url = window.prompt("링크 URL을 입력하세요 (https://...)");
		if (!url) return;
		applyFormat(el, setBody, insertLink(el.value, el.selectionStart, el.selectionEnd, url));
	}

	const toolbarButtons = [
		{ icon: "format_bold", title: "굵게", onClick: onBold },
		{ icon: "format_italic", title: "기울임", onClick: onItalic },
		{ icon: "format_list_bulleted", title: "목록", onClick: onList },
		{ icon: "image", title: "이미지·동영상·PDF 추가", onClick: () => fileInputRef.current?.click() },
		{ icon: "link", title: "링크 추가", onClick: onLink },
	];

	function addPollOption() {
		setPollOptions((prev) => [...prev, ""]);
	}
	function removePollOption(index: number) {
		setPollOptions((prev) => prev.filter((_, i) => i !== index));
	}

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		const trimmedTitle = title.trim();
		const trimmedBody = body.trim();

		if (!projectId) {
			setFieldError("프로젝트를 선택해 주세요.");
			return;
		}
		if (!trimmedTitle || !trimmedBody) {
			setFieldError("제목과 본문을 입력해 주세요.");
			return;
		}
		const trimmedPollOptions = pollOptions.map((o) => o.trim()).filter(Boolean);
		if (pollEnabled && (!pollQuestion.trim() || !pollEndAt || trimmedPollOptions.length < 2)) {
			setFieldError("투표 질문, 선택지 2개 이상, 종료 일시를 모두 입력해 주세요.");
			return;
		}
		setFieldError(undefined);

		const feedId = await run(async () => {
			const created = await createFeed({
				projectId: Number(projectId),
				title: trimmedTitle,
				content: trimmedBody,
				visibility,
			});
			try {
				for (const attachment of attachments) {
					await addFeedAttachment(created.feedId, attachment.file);
				}
				if (pollEnabled) {
					await createFeedPoll(created.feedId, {
						question: pollQuestion.trim(),
						endAt: endOfDayToIso(pollEndAt),
						options: trimmedPollOptions,
					});
				}
			} catch (err) {
				// 첨부·투표 실패 시 재시도하면 피드가 중복 생성되므로, 방금 만든 피드를 되돌린다.
				await deleteFeed(created.feedId).catch(() => {});
				throw err;
			}
			return created.feedId;
		});

		if (feedId) {
			navigate(paths.dashboardPosts);
		}
	}

	if (!projectsLoading && !projectsError && projects.length === 0) {
		return (
			<div className="container-page py-8">
				<h1 className="mb-6 text-headline-lg font-display text-on-surface">새 게시물 작성</h1>
				<p className="py-8 text-center text-body-md text-secondary">
					아직 등록된 프로젝트가 없어요. 게시물을 작성하려면 프로젝트가 먼저 필요합니다.
				</p>
			</div>
		);
	}

	return (
		<form
			onSubmit={onSubmit}
			noValidate
			className="container-page grid grid-cols-1 gap-gutter py-8 lg:grid-cols-[1fr_360px]"
		>
			<div>
				<h1 className="mb-6 text-headline-lg font-display text-on-surface">새 게시물 작성</h1>

				{serverError && <Alert className="mb-5">{serverError}</Alert>}
				{fieldError && <Alert className="mb-5">{fieldError}</Alert>}
				{projectsError && <Alert className="mb-5">{projectsError}</Alert>}

				<div className="mb-5 flex flex-col gap-2">
					<label className="text-label-md font-label-md text-on-surface" htmlFor="project-select">
						프로젝트
					</label>
					<select
						id="project-select"
						value={projectId}
						onChange={(e) => setProjectId(e.target.value)}
						disabled={projectsLoading}
						className="h-11 rounded border border-outline-variant bg-surface-container-lowest px-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					>
						{projectsLoading && <option value="">프로젝트 불러오는 중…</option>}
						{projects.map((p) => (
							<option key={p.projectId} value={p.projectId}>
								{p.title}
							</option>
						))}
					</select>
				</div>

				<Input
					label="제목"
					value={title}
					onChange={(e) => setTitle(e.target.value)}
					placeholder="게시물 제목을 입력하세요"
					className="mb-5"
				/>

				<div>
					<label className="mb-2 block text-label-md font-label-md text-on-surface">본문</label>
					<div className="rounded-lg border border-outline-variant bg-surface-container-lowest">
						<div className="flex gap-1 rounded-t-lg border-b border-outline-variant bg-surface-container-low p-2">
							{toolbarButtons.map((btn) => (
								<button
									key={btn.icon}
									type="button"
									onClick={btn.onClick}
									title={btn.title}
									aria-label={btn.title}
									className="flex h-8 w-8 items-center justify-center rounded text-secondary hover:bg-surface-container"
								>
									<Icon name={btn.icon} className="text-[18px]" />
								</button>
							))}
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
							ref={bodyRef}
							value={body}
							onChange={(e) => setBody(e.target.value)}
							rows={10}
							placeholder="작업 과정을 자유롭게 기록해보세요. 툴바의 이미지 아이콘으로 사진·동영상·PDF 파일을 첨부할 수 있어요."
							className={cn(
								"w-full border-0 bg-transparent p-4 text-body-md focus:outline-none focus:ring-0",
								attachments.length === 0 && "rounded-b-lg",
							)}
						/>
						{attachments.length > 0 && (
							<div className="flex flex-wrap gap-3 rounded-b-lg border-t border-outline-variant p-3">
								{attachments.map((a) => {
									const isImage = a.file.type.startsWith("image/");
									const isVideo = a.file.type.startsWith("video/");
									return (
										<div key={a.id} className="relative h-20 w-20 overflow-hidden rounded-lg">
											{isImage ? (
												<img src={a.previewUrl} alt="첨부 미리보기" className="h-full w-full object-cover" />
											) : isVideo ? (
												<div className="relative h-full w-full bg-black">
													<video src={a.previewUrl} className="h-full w-full object-cover" muted />
													<div className="absolute inset-0 flex items-center justify-center bg-black/20">
														<Icon name="play_circle" className="text-[28px] text-white" />
													</div>
												</div>
											) : (
												<div className="flex h-full w-full flex-col items-center justify-center gap-1 bg-surface-container-low p-1.5 text-center">
													<Icon name="description" className="text-[24px] text-secondary" />
													<span className="line-clamp-2 break-all text-[10px] leading-tight text-secondary">
														{a.file.name}
													</span>
												</div>
											)}
											<button
												type="button"
												onClick={() => removeAttachment(a.id)}
												aria-label="첨부 삭제"
												className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-inverse-surface/80 text-inverse-on-surface"
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
			</div>

			<aside className="flex flex-col gap-6">
				<div className="flex gap-2">
					<Button
						type="button"
						variant="secondary"
						fullWidth
						disabled={loading}
						onClick={() => navigate(paths.dashboardPosts)}
					>
						취소
					</Button>
					<Button type="submit" fullWidth disabled={loading}>
						{loading ? "게시하는 중…" : "게시하기"}
					</Button>
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

				<div className="rounded-lg border border-outline-variant p-4">
					<div className="mb-3 flex items-center justify-between">
						<p className="text-label-md font-label-md text-on-surface">투표 생성</p>
						<button
							type="button"
							role="switch"
							aria-checked={pollEnabled}
							onClick={() => setPollEnabled((v) => !v)}
							className={cn(
								"relative h-6 w-11 rounded-full transition-colors",
								pollEnabled ? "bg-primary" : "bg-outline-variant",
							)}
						>
							<span
								className={cn(
									"absolute left-0.5 top-0.5 h-5 w-5 rounded-full bg-surface-container-lowest transition-transform",
									pollEnabled ? "translate-x-5" : "translate-x-0",
								)}
							/>
						</button>
					</div>
					<div className={cn("flex flex-col gap-3", !pollEnabled && "pointer-events-none opacity-50")}>
						<Input
							label="질문"
							value={pollQuestion}
							onChange={(e) => setPollQuestion(e.target.value)}
							placeholder="구독자에게 물어볼 질문"
						/>
						{pollOptions.map((opt, i) => (
							<div key={i} className="flex items-center gap-2">
								<input
									value={opt}
									onChange={(e) =>
										setPollOptions((prev) => prev.map((o, idx) => (idx === i ? e.target.value : o)))
									}
									placeholder={`선택지 ${i + 1}`}
									className="h-10 flex-1 rounded border border-outline-variant bg-surface-container-lowest px-3 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
								/>
								{pollOptions.length > 2 && (
									<button
										type="button"
										onClick={() => removePollOption(i)}
										aria-label="선택지 삭제"
										className="text-secondary hover:text-error"
									>
										<Icon name="delete" className="text-[18px]" />
									</button>
								)}
							</div>
						))}
						<button
							type="button"
							onClick={addPollOption}
							className="text-left text-label-md font-label-md text-primary hover:underline"
						>
							+ 선택지 추가
						</button>
						<Input
							label="투표 종료일"
							type="date"
							value={pollEndAt}
							onChange={(e) => setPollEndAt(e.target.value)}
						/>
						<p className="text-caption font-caption text-secondary">
							선택한 날짜 자정(24시)에 자동으로 마감돼요. 유료 구독자는 투표에서 2표가 반영됩니다.
						</p>
					</div>
				</div>
			</aside>
		</form>
	);
}
