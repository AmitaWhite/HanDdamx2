import { useRef, useState } from "react";

import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Icon } from "@/components/ui/Icon";
import { useAuth } from "@/features/auth/AuthContext";
import { deleteProfileImage, updateProfileImage } from "@/features/member/memberApi";
import type { MemberProfileResponse } from "@/features/member/types";
import { asApiError } from "@/lib/api";

const ACCEPTED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];
const MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB TODO: 팀 합의 후 결정

interface ProfileImageFormProps {
	profile: MemberProfileResponse;
	onProfileUpdated: (profile: MemberProfileResponse) => void;
}

function validateImageFile(file: File): string | null {
	if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
		return "이미지 파일(JPG, PNG, GIF, WEBP)만 업로드할 수 있습니다.";
	}
	if (file.size > MAX_FILE_SIZE_BYTES) {
		return "5MB 이하의 이미지만 업로드할 수 있습니다.";
	}
	return null;
}

export function ProfileImageForm({ profile, onProfileUpdated }: ProfileImageFormProps) {
	const { updateUser } = useAuth();
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [confirmOpen, setConfirmOpen] = useState(false);
	const fileInputRef = useRef<HTMLInputElement>(null);

	function openFilePicker() {
		fileInputRef.current?.click();
	}

	async function onFileChange(e: React.ChangeEvent<HTMLInputElement>) {
		const file = e.target.files?.[0];
		e.target.value = ""; // 같은 파일 재선택 가능하도록

		if (!file) return;

		const validationError = validateImageFile(file);
		if (validationError) {
			setError(validationError);
			return;
		}

		setError(null);
		setSaving(true);
		try {
			const updated = await updateProfileImage(file);
			onProfileUpdated(updated);
			updateUser({ profileImageUrl: updated.profileImageUrl });
		} catch (err) {
			setError(asApiError(err).message);
		} finally {
			setSaving(false);
		}
	}

	async function onDelete() {
		setConfirmOpen(false);
		setError(null);
		setSaving(true);
		try {
			const updated = await deleteProfileImage();
			onProfileUpdated(updated);
			updateUser({ profileImageUrl: updated.profileImageUrl });
		} catch (err) {
			setError(asApiError(err).message);
		} finally {
			setSaving(false);
		}
	}

	return (
		<div className="mb-8">
			<div className="flex items-center gap-4">
				<div className="relative">
					<Avatar
						src={profile.profileImageUrl ?? undefined}
						alt={profile.nickname}
						fallbackText={profile.nickname}
						size={80}
					/>
					<button
						type="button"
						onClick={openFilePicker}
						disabled={saving}
						className="absolute -bottom-1 -right-1 flex h-7 w-7 items-center justify-center rounded-full bg-primary text-on-primary disabled:opacity-50"
						aria-label="프로필 사진 변경"
					>
						<Icon name="photo_camera" className="text-[16px]" />
					</button>
					<input
						ref={fileInputRef}
						type="file"
						accept={ACCEPTED_IMAGE_TYPES.join(",")}
						className="hidden"
						onChange={onFileChange}
					/>
				</div>

				<Button type="button" variant="secondary" size="sm" disabled={saving} onClick={openFilePicker}>
					{saving ? "처리 중…" : "사진 변경"}
				</Button>

				{profile.profileImageUrl ? (
					<Button
						type="button"
						variant="ghost"
						size="sm"
						disabled={saving}
						onClick={() => setConfirmOpen(true)}
					>
						<Icon name="delete" className="text-[18px]" />
						사진 삭제
					</Button>
				) : null}
			</div>

			{error && (
				<p role="alert" className="mt-2 text-caption font-caption text-error">
					{error}
				</p>
			)}

			<ConfirmDialog
				open={confirmOpen}
				title="프로필 사진을 삭제할까요?"
				confirmLabel="삭제"
				onConfirm={onDelete}
				onCancel={() => setConfirmOpen(false)}
			/>
		</div>
	);
}
