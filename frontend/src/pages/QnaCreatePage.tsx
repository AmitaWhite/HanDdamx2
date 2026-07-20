import { useMemo, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { Link, Navigate, useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import {
	type BoardPostType,
	createPremiumBoardPost,
} from "@/features/board/boardApi";
import { useSubmitState } from "@/features/auth/useSubmitState";
import { findCreator } from "@/mocks/creators";
import type { QnaCategory } from "@/mocks/qna";

const CATEGORY_OPTIONS: { label: QnaCategory; type: BoardPostType }[] = [
	{ label: "제작 질문", type: "QUESTION" },
	{ label: "작품 피드백", type: "FEEDBACK" },
	{ label: "콘텐츠 제안", type: "CONTENT_SUGGESTION" },
	{ label: "재료 추천", type: "MATERIAL" },
	{ label: "일반 소통", type: "GENERAL" },
];

function toNumericCreatorId(value: string): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isInteger(n) && n > 0 ? n : null;
}

/**
 * 유료 Q&A 글쓰기 — LDJ-002 POST /api/creators/{creatorId}/premium-board/posts
 */
export function QnaCreatePage() {
	const { creatorId = "" } = useParams();
	const navigate = useNavigate();
	const creator = findCreator(creatorId);
	const numericCreatorId = toNumericCreatorId(creatorId);

	const [title, setTitle] = useState("");
	const [type, setType] = useState<BoardPostType>("QUESTION");
	const [content, setContent] = useState("");
	const [files, setFiles] = useState<File[]>([]);
	const [fieldError, setFieldError] = useState<string | undefined>();

	const {
		loading,
		error: serverError,
		run,
	} = useSubmitState("글 작성에 실패했습니다. 다시 시도해 주세요.");

	const previews = useMemo(
		() =>
			files.map((file) => ({
				name: file.name,
				url: URL.createObjectURL(file),
			})),
		[files],
	);

	if (numericCreatorId === null) {
		return <Navigate to={paths.creatorQna(creatorId || "suyeon")} replace />;
	}

	// early return 이후에도 중첩 함수 안에서는 null 이 남을 수 있어 number 로 고정
	const creatorMemberId: number = numericCreatorId;

	function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
		const selected = e.target.files;
		if (!selected) return;
		setFiles((prev) => [...prev, ...Array.from(selected)]);
		e.target.value = "";
	}

	function removeFile(index: number) {
		setFiles((prev) => prev.filter((_, i) => i !== index));
	}

	async function onSubmit(e: FormEvent) {
		e.preventDefault();
		const trimmedTitle = title.trim();
		const trimmedContent = content.trim();
		if (!trimmedTitle || !trimmedContent) {
			setFieldError("제목과 내용을 입력해 주세요.");
			return;
		}
		setFieldError(undefined);

		const created = await run(() =>
			createPremiumBoardPost({
				creatorId: creatorMemberId,
				title: trimmedTitle,
				type,
				content: trimmedContent,
				images: files,
			}),
		);
		if (created) {
			navigate(paths.creatorQna(creatorMemberId), { replace: true });
		}
	}

	return (
		<div className="container-page max-w-2xl py-6">
			<Link
				to={paths.creatorQna(creatorMemberId)}
				className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				Q&A 게시판
			</Link>

			<h1 className="mb-1 text-headline-md font-display text-on-surface">
				{creator.name} 작가 · 글쓰기
			</h1>
			<p className="mb-6 text-caption font-caption text-secondary">
				유료 구독자 전용 Q&A에 글을 남깁니다
			</p>

			<form className="flex flex-col gap-5" onSubmit={onSubmit} noValidate>
				{serverError && <Alert>{serverError}</Alert>}
				{fieldError && <Alert>{fieldError}</Alert>}

				<div className="flex flex-col gap-1.5">
					<label
						htmlFor="qna-type"
						className="text-label-md font-label-md text-on-surface"
					>
						카테고리
					</label>
					<select
						id="qna-type"
						value={type}
						onChange={(e) => setType(e.target.value as BoardPostType)}
						className="h-11 rounded border border-outline-variant bg-surface-container-lowest px-4 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					>
						{CATEGORY_OPTIONS.map((opt) => (
							<option key={opt.type} value={opt.type}>
								{opt.label}
							</option>
						))}
					</select>
				</div>

				<Input
					label="제목"
					value={title}
					onChange={(e) => setTitle(e.target.value)}
					placeholder="제목을 입력하세요"
					maxLength={255}
					required
				/>

				<div className="flex flex-col gap-1.5">
					<label
						htmlFor="qna-content"
						className="text-label-md font-label-md text-on-surface"
					>
						내용
					</label>
					<textarea
						id="qna-content"
						value={content}
						onChange={(e) => setContent(e.target.value)}
						placeholder="질문이나 의견을 적어 주세요"
						rows={8}
						required
						className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-4 py-3 text-body-md placeholder:text-outline focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
					/>
				</div>

				<div>
					<label className="mb-2 block text-label-md font-label-md text-on-surface">
						이미지 (선택)
					</label>
					<label className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-outline-variant bg-surface-container-low py-8 text-center hover:border-primary">
						<Icon name="upload" className="text-[28px] text-secondary" />
						<span className="text-body-md text-secondary">
							이미지 파일을 선택하세요
						</span>
						<input
							type="file"
							multiple
							accept="image/*"
							className="hidden"
							onChange={onFilesSelected}
						/>
					</label>
					{previews.length > 0 && (
						<ul className="mt-3 flex flex-wrap gap-3">
							{previews.map((preview, index) => (
								<li key={`${preview.name}-${index}`} className="relative">
									<img
										src={preview.url}
										alt={preview.name}
										className="h-24 w-24 rounded-lg object-cover"
									/>
									<button
										type="button"
										onClick={() => removeFile(index)}
										className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-on-surface text-surface-container-lowest"
										aria-label={`${preview.name} 제거`}
									>
										<Icon name="close" className="text-[14px]" />
									</button>
								</li>
							))}
						</ul>
					)}
				</div>

				<div className="flex gap-3 pt-2">
					<Button
						type="button"
						variant="secondary"
						fullWidth
						onClick={() => navigate(paths.creatorQna(creatorMemberId))}
						disabled={loading}
					>
						취소
					</Button>
					<Button type="submit" fullWidth disabled={loading}>
						{loading ? "등록 중…" : "등록하기"}
					</Button>
				</div>
			</form>
		</div>
	);
}
