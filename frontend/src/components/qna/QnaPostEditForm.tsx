import type { ChangeEvent, FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import type { BoardPostResponse, BoardPostType } from "@/features/board/boardApi";
import { CATEGORY_OPTIONS } from "@/features/qna/qnaConstants";

interface QnaPostEditFormProps {
	post: BoardPostResponse;
	editTitle: string;
	editType: BoardPostType;
	editContent: string;
	saving: boolean;
	imageBusy: boolean;
	onTitleChange: (value: string) => void;
	onTypeChange: (type: BoardPostType) => void;
	onContentChange: (value: string) => void;
	onSave: (e: FormEvent) => void;
	onCancel: () => void;
	onAddImages: (e: ChangeEvent<HTMLInputElement>) => void;
	onRemoveImage: (imageId: number) => void;
}

export function QnaPostEditForm({
	post,
	editTitle,
	editType,
	editContent,
	saving,
	imageBusy,
	onTitleChange,
	onTypeChange,
	onContentChange,
	onSave,
	onCancel,
	onAddImages,
	onRemoveImage,
}: QnaPostEditFormProps) {
	return (
		<form className="flex flex-col gap-4" onSubmit={onSave} noValidate>
			<div className="flex flex-col gap-1.5">
				<label
					htmlFor="edit-type"
					className="text-label-md font-label-md text-on-surface"
				>
					카테고리
				</label>
				<select
					id="edit-type"
					value={editType}
					onChange={(e) => onTypeChange(e.target.value as BoardPostType)}
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
				value={editTitle}
				onChange={(e) => onTitleChange(e.target.value)}
				maxLength={255}
				required
			/>
			<div className="flex flex-col gap-1.5">
				<label
					htmlFor="edit-content"
					className="text-label-md font-label-md text-on-surface"
				>
					내용
				</label>
				<textarea
					id="edit-content"
					value={editContent}
					onChange={(e) => onContentChange(e.target.value)}
					rows={8}
					required
					className="w-full resize-y rounded border border-outline-variant bg-surface-container-lowest px-4 py-3 text-body-md focus:border-on-surface focus:outline-none focus:ring-1 focus:ring-on-surface"
				/>
			</div>

			<div>
				<label className="mb-2 block text-label-md font-label-md text-on-surface">
					이미지
				</label>
				{post.images.length > 0 && (
					<ul className="mb-3 flex flex-wrap gap-3">
						{[...post.images]
							.sort((a, b) => a.orderIndex - b.orderIndex)
							.map((image) => (
								<li key={image.id} className="relative">
									<img
										src={image.url}
										alt={image.originalName ?? "첨부 이미지"}
										className="h-24 w-24 rounded-lg object-cover"
									/>
									<button
										type="button"
										onClick={() => onRemoveImage(image.id)}
										disabled={imageBusy}
										className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full bg-on-surface text-surface-container-lowest disabled:opacity-50"
										aria-label="이미지 삭제"
									>
										<Icon name="close" className="text-[14px]" />
									</button>
								</li>
							))}
					</ul>
				)}
				<label className="flex cursor-pointer flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-outline-variant bg-surface-container-low py-6 text-center hover:border-primary">
					<Icon name="upload" className="text-[24px] text-secondary" />
					<span className="text-body-md text-secondary">
						{imageBusy ? "업로드 중…" : "이미지 추가"}
					</span>
					<input
						type="file"
						multiple
						accept="image/*"
						className="hidden"
						disabled={imageBusy || saving}
						onChange={onAddImages}
					/>
				</label>
			</div>

			<div className="flex gap-3">
				<Button
					type="button"
					variant="secondary"
					fullWidth
					onClick={onCancel}
					disabled={saving || imageBusy}
				>
					취소
				</Button>
				<Button type="submit" fullWidth disabled={saving || imageBusy}>
					{saving ? "저장 중…" : "저장"}
				</Button>
			</div>
		</form>
	);
}
