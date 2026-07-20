import { useEffect, useId, useRef, useState } from "react";
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

const ALLOWED_IMAGE_TYPES = new Set([
	"image/jpeg",
	"image/png",
	"image/gif",
	"image/webp",
	"image/jpg",
]);

type ImageItem = {
	id: string;
	file: File;
	previewUrl: string;
};

function toNumericCreatorId(value: string): number | null {
	if (!value) return null;
	const n = Number(value);
	return Number.isInteger(n) && n > 0 ? n : null;
}

function isAllowedImage(file: File): boolean {
	if (file.type && ALLOWED_IMAGE_TYPES.has(file.type.toLowerCase())) {
		return true;
	}
	// Windows 등에서 type 이 비어 있는 경우 확장자로 허용
	const name = file.name.toLowerCase();
	return (
		name.endsWith(".jpg") ||
		name.endsWith(".jpeg") ||
		name.endsWith(".png") ||
		name.endsWith(".gif") ||
		name.endsWith(".webp")
	);
}

/**
 * 유료 Q&A 글쓰기 — LDJ-002 POST /api/creators/{creatorId}/premium-board/posts
 */
export function QnaCreatePage() {
	const { creatorId = "" } = useParams();
	const navigate = useNavigate();
	const creator = findCreator(creatorId);
	const numericCreatorId = toNumericCreatorId(creatorId);
	const fileInputId = useId();
	const fileInputRef = useRef<HTMLInputElement>(null);
	const imagesRef = useRef<ImageItem[]>([]);

	const [title, setTitle] = useState("");
	const [type, setType] = useState<BoardPostType>("QUESTION");
	const [content, setContent] = useState("");
	const [images, setImages] = useState<ImageItem[]>([]);
	const [fieldError, setFieldError] = useState<string | undefined>();

	const {
		loading,
		error: serverError,
		run,
	} = useSubmitState("글 작성에 실패했습니다. 다시 시도해 주세요.");

	imagesRef.current = images;

	// 언마운트 시에만 preview URL 정리 (StrictMode useEffect revoke 레이스 방지)
	useEffect(() => {
		return () => {
			for (const item of imagesRef.current) {
				URL.revokeObjectURL(item.previewUrl);
			}
		};
	}, []);

	if (numericCreatorId === null) {
		return <Navigate to={paths.creatorQna(creatorId || "suyeon")} replace />;
	}

	const creatorMemberId: number = numericCreatorId;

	function onFilesSelected(e: ChangeEvent<HTMLInputElement>) {
		const selected = e.target.files;
		if (!selected || selected.length === 0) return;

		const accepted: ImageItem[] = [];
		const rejected: string[] = [];

		for (const file of Array.from(selected)) {
			if (!isAllowedImage(file)) {
				rejected.push(file.name);
				continue;
			}
			accepted.push({
				id: `${file.name}-${file.size}-${file.lastModified}-${crypto.randomUUID()}`,
				file,
				previewUrl: URL.createObjectURL(file),
			});
		}

		if (accepted.length > 0) {
			setImages((prev) => [...prev, ...accepted]);
			setFieldError(undefined);
		}
		if (rejected.length > 0) {
			setFieldError(
				`지원하지 않는 파일입니다: ${rejected.join(", ")} (jpg/png/gif/webp만)`,
			);
		}

		e.target.value = "";
	}

	function removeImage(id: string) {
		setImages((prev) => {
			const target = prev.find((item) => item.id === id);
			if (target) URL.revokeObjectURL(target.previewUrl);
			return prev.filter((item) => item.id !== id);
		});
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

		const files = images.map((item) => item.file);
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
			navigate(paths.qnaPost(created.id), {
				replace: true,
				state:
					files.length > 0 && created.images.length === 0
						? { imageUploadFailed: true }
						: undefined,
			});
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
					<p className="mb-2 text-label-md font-label-md text-on-surface">
						이미지 (선택)
						{images.length > 0 ? ` · ${images.length}장` : ""}
					</p>
					<input
						ref={fileInputRef}
						id={fileInputId}
						type="file"
						multiple
						accept="image/*"
						className="absolute h-px w-px overflow-hidden opacity-0"
						onChange={onFilesSelected}
					/>
					<button
						type="button"
						onClick={() => fileInputRef.current?.click()}
						className="flex w-full cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-outline-variant bg-surface-container-low py-8 text-center hover:border-primary"
					>
						<Icon name="upload" className="text-[28px] text-secondary" />
						<span className="text-body-md text-secondary">
							이미지 파일을 선택하세요
						</span>
						<span className="text-caption font-caption text-outline">
							jpg, png, gif, webp
						</span>
					</button>
					{images.length > 0 && (
						<ul className="mt-3 flex flex-wrap gap-3">
							{images.map((item) => (
								<li key={item.id} className="relative">
									<img
										src={item.previewUrl}
										alt={item.file.name}
										className="h-24 w-24 rounded-lg object-cover"
									/>
									<button
										type="button"
										onClick={() => removeImage(item.id)}
										className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-on-surface text-surface-container-lowest"
										aria-label={`${item.file.name} 제거`}
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
