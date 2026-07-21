import { useState } from "react";

import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { PASSWORD_HINT, validatePassword } from "@/features/auth/validation";
import { changePassword } from "@/features/member/memberApi";
import { asApiError } from "@/lib/api";

export function PasswordForm() {
	const [currentPassword, setCurrentPassword] = useState("");
	const [newPassword, setNewPassword] = useState("");
	const [showCurrent, setShowCurrent] = useState(false);
	const [showNew, setShowNew] = useState(false);
	const [saving, setSaving] = useState(false);
	const [currentPasswordError, setCurrentPasswordError] = useState<string | null>(null);
	const [newPasswordError, setNewPasswordError] = useState<string | null>(null);
	const [formError, setFormError] = useState<string | null>(null);
	const [saved, setSaved] = useState(false);

	function clearErrors() {
		setCurrentPasswordError(null);
		setNewPasswordError(null);
		setFormError(null);
	}

	async function onSubmit(e: React.FormEvent) {
		e.preventDefault();
		clearErrors();
		setSaved(false);

		const newPasswordErr = validatePassword(newPassword);
		if (newPasswordErr) {
			setNewPasswordError(newPasswordErr);
			return;
		}

		setSaving(true);
		try {
			await changePassword(currentPassword, newPassword);
			setCurrentPassword("");
			setNewPassword("");
			setSaved(true);
		} catch (err) {
			const apiErr = asApiError(err);
			if (apiErr.code === "OAUTH_MEMBER_CANNOT_CHANGE_PASSWORD") {
				setFormError("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.");
			} else if (apiErr.code === "INVALID_CURRENT_PASSWORD") {
				setCurrentPasswordError("현재 비밀번호가 일치하지 않습니다.");
			} else if (apiErr.code === "INVALID_REQUEST") {
				setNewPasswordError(PASSWORD_HINT);
			} else {
				setFormError(apiErr.message);
			}
		} finally {
			setSaving(false);
		}
	}

	return (
		<form className="flex max-w-md flex-col gap-5" onSubmit={onSubmit}>
			<div className="relative">
				<Input
					label="현재 비밀번호"
					type={showCurrent ? "text" : "password"}
					placeholder="••••••••"
					value={currentPassword}
					disabled={saving}
					onChange={(e) => {
						setCurrentPassword(e.target.value);
						setSaved(false);
						setCurrentPasswordError(null);
						setFormError(null);
					}}
					error={currentPasswordError ?? undefined}
				/>
				<button
					type="button"
					onClick={() => setShowCurrent((v) => !v)}
					className="absolute right-3 top-9 text-secondary"
					aria-label={showCurrent ? "비밀번호 숨기기" : "비밀번호 표시"}
				>
					<Icon name={showCurrent ? "visibility_off" : "visibility"} className="text-[20px]" />
				</button>
			</div>

			<div className="relative">
				<Input
					label="새 비밀번호"
					type={showNew ? "text" : "password"}
					placeholder="8자 이상 입력"
					value={newPassword}
					disabled={saving}
					onChange={(e) => {
						setNewPassword(e.target.value);
						setSaved(false);
						setNewPasswordError(null);
						setFormError(null);
					}}
					error={newPasswordError ?? undefined}
				/>
				<button
					type="button"
					onClick={() => setShowNew((v) => !v)}
					className="absolute right-3 top-9 text-secondary"
					aria-label={showNew ? "비밀번호 숨기기" : "비밀번호 표시"}
				>
					<Icon name={showNew ? "visibility_off" : "visibility"} className="text-[20px]" />
				</button>
				{!newPasswordError && (
					<p className="mt-1.5 text-caption font-caption text-secondary">
						{PASSWORD_HINT}
					</p>
				)}
			</div>

			{formError && (
				<p role="alert" className="rounded bg-error/10 px-4 py-3 text-label-md font-label-md text-error">
					{formError}
				</p>
			)}

			{saved && (
				<p role="status" className="rounded bg-primary/10 px-4 py-3 text-label-md font-label-md text-primary">
					비밀번호가 변경되었습니다.
				</p>
			)}

			<div className="mt-2">
				<Button type="submit" disabled={saving}>
					{saving ? "변경 중…" : "비밀번호 변경"}
				</Button>
			</div>
		</form>
	);
}