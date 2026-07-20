import { useState } from "react";

import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";

export function PasswordForm() {
	const [currentPassword, setCurrentPassword] = useState("");
	const [newPassword, setNewPassword] = useState("");
	const [showCurrent, setShowCurrent] = useState(false);
	const [showNew, setShowNew] = useState(false);

	function onSubmit(e: React.FormEvent) {
		e.preventDefault();

		// 커밋 4에서 API 연동
	}

	return (
		<form className="flex max-w-md flex-col gap-5" onSubmit={onSubmit}>
			<div className="relative">
				<Input
					label="현재 비밀번호"
					type={showCurrent ? "text" : "password"}
					placeholder="••••••••"
					value={currentPassword}
					onChange={(e) => setCurrentPassword(e.target.value)}
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
					onChange={(e) => setNewPassword(e.target.value)}
				/>
				<button
					type="button"
					onClick={() => setShowNew((v) => !v)}
					className="absolute right-3 top-9 text-secondary"
					aria-label={showNew ? "비밀번호 숨기기" : "비밀번호 표시"}
				>
					<Icon name={showNew ? "visibility_off" : "visibility"} className="text-[20px]" />
				</button>
			</div>

			<div className="mt-2">
				<Button type="submit">변경사항 저장</Button>
			</div>
		</form>
	);
}