import { useState } from "react";

import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { updateMyProfile } from "@/features/member/memberApi";
import type { MemberProfileResponse } from "@/features/member/types";
import { asApiError } from "@/lib/api";

interface NicknameFormProps {
	initialNickname: string;
	onProfileUpdated: (profile: MemberProfileResponse) => void;
}

export function NicknameForm({ initialNickname, onProfileUpdated }: NicknameFormProps) {
	const [nickname, setNickname] = useState(initialNickname);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);
	const [saved, setSaved] = useState(false);

	async function onSubmit(e: React.FormEvent) {
		e.preventDefault();
		setError(null);
		setSaved(false);
		setSaving(true);
		try {
			const updated = await updateMyProfile(nickname);
			setNickname(updated.nickname);
			setError(null);
			onProfileUpdated(updated);
			setSaved(true);
		} catch (err) {
			const apiErr = asApiError(err);
			setError(
				apiErr.code === "DUPLICATE_NICKNAME"
					? "이미 사용 중인 닉네임입니다."
					: "닉네임 변경에 실패했습니다.",
			);
		} finally {
			setSaving(false);
		}
	}

	return (
		<form className="mb-5 flex max-w-md flex-col gap-5" onSubmit={onSubmit}>
			<Input
				label="닉네임"
				value={nickname}
				disabled={saving}
				onChange={(e) => {
					setNickname(e.target.value);
					setSaved(false);
				}}
				error={error ?? undefined}
			/>

			{saved && (
				<p role="status" className="rounded bg-primary/10 px-4 py-3 text-label-md font-label-md text-primary">
					닉네임이 저장되었습니다.
				</p>
			)}

			<Button type="submit" disabled={saving}>
				{saving ? "저장 중…" : "닉네임 변경"}
			</Button>
		</form>
	);
}