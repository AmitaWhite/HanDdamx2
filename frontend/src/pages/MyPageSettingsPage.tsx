import { useState } from "react";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { LinkButton } from "@/components/ui/LinkButton";
import { mockImg } from "@/mocks/helpers";
import { mockMember } from "@/mocks/member";

export function MyPageSettingsPage() {
	const [nickname, setNickname] = useState(mockMember.nickname);
	const [currentPassword, setCurrentPassword] = useState("");
	const [newPassword, setNewPassword] = useState("");
	const [showCurrent, setShowCurrent] = useState(false);
	const [showNew, setShowNew] = useState(false);
	const [saved, setSaved] = useState(false);

	function onSubmit(e: React.FormEvent) {
		e.preventDefault();
		setSaved(true);
	}

	return (
		<MyPageShell>
			<h1 className="mb-6 border-b border-outline-variant pb-4 text-headline-lg font-display text-on-surface">
				프로필 설정
			</h1>

			{saved && (
				<p role="status" className="mb-6 rounded bg-primary/10 px-4 py-3 text-label-md font-label-md text-primary">
					변경사항이 저장되었습니다. (목데이터 — 실제 저장되지 않음)
				</p>
			)}

			<div className="mb-8 flex items-center gap-4">
				<div className="relative">
					<Avatar src={mockImg(mockMember.avatarSeed, 160, 160)} size={80} />
					<span className="absolute -bottom-1 -right-1 flex h-7 w-7 items-center justify-center rounded-full bg-primary text-on-primary">
						<Icon name="photo_camera" className="text-[16px]" />
					</span>
				</div>
				<Button type="button" variant="secondary" size="sm">
					사진 변경
				</Button>
			</div>

			<form className="flex max-w-md flex-col gap-5" onSubmit={onSubmit}>
				<Input label="닉네임" value={nickname} onChange={(e) => setNickname(e.target.value)} />

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

				<div className="mt-2 flex gap-3">
					<Button type="submit">변경사항 저장</Button>
					<LinkButton to={paths.mypage} variant="ghost">
						취소
					</LinkButton>
				</div>
			</form>
		</MyPageShell>
	);
}
