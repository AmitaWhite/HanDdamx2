import { useState } from "react";
import type { ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { cn } from "@/lib/cn";
import { mockProjects, PROJECT_STAGES } from "@/mocks/projects";

const CURRENT_CREATOR_ID = "suyeon";

type Visibility = "public" | "free" | "paid";

const VISIBILITY_OPTIONS: { id: Visibility; label: string; description: string }[] = [
	{ id: "public", label: "전체공개", description: "누구나 볼 수 있어요" },
	{ id: "free", label: "무료 구독자만", description: "무료 이상 구독자에게 공개" },
	{ id: "paid", label: "유료 구독자만", description: "유료 구독자에게만 공개" },
];

const TOOLBAR_ICONS = ["format_bold", "format_italic", "format_list_bulleted", "image", "link"];

export function CreatePostPage() {
	const navigate = useNavigate();
	const myProjects = mockProjects.filter((p) => p.creatorId === CURRENT_CREATOR_ID);

	const [projectId, setProjectId] = useState("");
	const [title, setTitle] = useState("");
	const [body, setBody] = useState("");
	const [attachments, setAttachments] = useState<{ id: string; url: string }[]>([]);
	const [visibility, setVisibility] = useState<Visibility>("free");
	const [stage, setStage] = useState("");
	const [pollEnabled, setPollEnabled] = useState(false);
	const [pollQuestion, setPollQuestion] = useState("");
	const [pollOptions, setPollOptions] = useState(["", ""]);
	const [pollEndAt, setPollEndAt] = useState("");

	function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
		const files = e.target.files;
		if (!files) return;
		const next = Array.from(files).map((file) => ({ id: `${file.name}-${Date.now()}`, url: URL.createObjectURL(file) }));
		setAttachments((prev) => [...prev, ...next]);
	}

	function removeAttachment(id: string) {
		setAttachments((prev) => prev.filter((a) => a.id !== id));
	}

	function addPollOption() {
		setPollOptions((prev) => [...prev, ""]);
	}
	function removePollOption(index: number) {
		setPollOptions((prev) => prev.filter((_, i) => i !== index));
	}

	function handlePublish() {
		navigate(paths.dashboardPosts);
	}

	return (
		<div className="container-page grid grid-cols-1 gap-gutter py-8 lg:grid-cols-[1fr_360px]">
			<div>
				<h1 className="mb-6 text-headline-lg font-display text-on-surface">새 게시물 작성</h1>

				<div className="mb-5 flex flex-col gap-2">
					<label className="text-label-md font-label-md text-on-surface" htmlFor="project-select">
						프로젝트
					</label>
					<select
						id="project-select"
						value={projectId}
						onChange={(e) => setProjectId(e.target.value)}
						className="h-11 rounded border border-outline-variant bg-surface-container-lowest px-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					>
						<option value="">프로젝트 선택 안 함</option>
						{myProjects.map((p) => (
							<option key={p.id} value={p.id}>
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

				<div className="mb-5">
					<label className="mb-2 block text-label-md font-label-md text-on-surface">첨부파일</label>
					<label className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-outline-variant bg-surface-container-low py-10 text-center hover:border-primary">
						<Icon name="upload" className="text-[28px] text-secondary" />
						<span className="text-body-md text-secondary">이미지·영상 링크·첨부파일(PDF 도안 등)</span>
						<input type="file" multiple className="hidden" onChange={onFilesSelected} accept="image/*" />
					</label>
					{attachments.length > 0 && (
						<div className="mt-3 flex flex-wrap gap-3">
							{attachments.map((a) => (
								<div key={a.id} className="relative h-20 w-20 overflow-hidden rounded-lg">
									<img src={a.url} alt="첨부 미리보기" className="h-full w-full object-cover" />
									<button
										type="button"
										onClick={() => removeAttachment(a.id)}
										aria-label="첨부 삭제"
										className="absolute right-1 top-1 flex h-5 w-5 items-center justify-center rounded-full bg-inverse-surface/80 text-inverse-on-surface"
									>
										<Icon name="close" className="text-[14px]" />
									</button>
								</div>
							))}
						</div>
					)}
				</div>

				<div>
					<label className="mb-2 block text-label-md font-label-md text-on-surface">본문</label>
					<div className="mb-2 flex gap-1 rounded-t-lg border border-b-0 border-outline-variant bg-surface-container-low p-2">
						{TOOLBAR_ICONS.map((icon) => (
							<button
								key={icon}
								type="button"
								className="flex h-8 w-8 items-center justify-center rounded text-secondary hover:bg-surface-container"
							>
								<Icon name={icon} className="text-[18px]" />
							</button>
						))}
					</div>
					<textarea
						value={body}
						onChange={(e) => setBody(e.target.value)}
						rows={10}
						placeholder="작업 과정을 자유롭게 기록해보세요"
						className="w-full rounded-b-lg border border-outline-variant bg-surface-container-lowest p-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					/>
				</div>
			</div>

			<aside className="flex flex-col gap-6">
				<div className="flex gap-2">
					<Button fullWidth onClick={handlePublish}>
						게시하기
					</Button>
					<Button type="button" variant="secondary" fullWidth>
						임시저장
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

				<div>
					<label className="mb-2 block text-label-md font-label-md text-on-surface" htmlFor="stage-select">
						프로젝트 단계
					</label>
					<select
						id="stage-select"
						value={stage}
						onChange={(e) => setStage(e.target.value)}
						className="h-11 w-full rounded border border-outline-variant bg-surface-container-lowest px-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					>
						<option value="">설정 안 함</option>
						{PROJECT_STAGES.map((s) => (
							<option key={s.id} value={s.id}>
								{s.ko}
							</option>
						))}
					</select>
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
									"absolute top-0.5 h-5 w-5 rounded-full bg-surface-container-lowest transition-transform",
									pollEnabled ? "translate-x-5" : "translate-x-0.5",
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
									onChange={(e) => setPollOptions((prev) => prev.map((o, idx) => (idx === i ? e.target.value : o)))}
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
						<button type="button" onClick={addPollOption} className="text-left text-label-md font-label-md text-primary hover:underline">
							+ 선택지 추가
						</button>
						<Input
							label="투표 종료 일시"
							type="datetime-local"
							value={pollEndAt}
							onChange={(e) => setPollEndAt(e.target.value)}
						/>
						<p className="text-caption font-caption text-secondary">유료 구독자는 투표에서 2표가 반영됩니다.</p>
					</div>
				</div>
			</aside>
		</div>
	);
}
