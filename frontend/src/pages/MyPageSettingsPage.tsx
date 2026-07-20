import { useEffect, useState } from "react";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { Input } from "@/components/ui/Input";
import { LinkButton } from "@/components/ui/LinkButton";
import { getMyProfile } from "@/features/member/memberApi";
import type { MemberProfileResponse } from "@/features/member/types";

export function MyPageSettingsPage() {
	const [profile, setProfile] = useState<MemberProfileResponse | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(false);

	const [nickname, setNickname] = useState("");
	const [currentPassword, setCurrentPassword] = useState("");
	const [newPassword, setNewPassword] = useState("");

	const [showCurrent, setShowCurrent] = useState(false);
	const [showNew, setShowNew] = useState(false);

	useEffect(() => {
		let ignore = false;

		const fetchProfile = async () => {
			try {
				const data = await getMyProfile();

				if (!ignore) {
					setProfile(data);
					setNickname(data.nickname);
				}
			} catch {
				if (!ignore) {
					setError(true);
				}
			} finally {
				if (!ignore) {
					setLoading(false);
				}
			}
		};

		fetchProfile();

		return () => {
			ignore = true;
		};
	}, []);

	function onSubmit(e: React.FormEvent) {
		e.preventDefault();
	}

	if (loading) {
		return (
			<MyPageShell>
				<p className="py-12 text-center text-body-md text-secondary">
					불러오는 중...
				</p>
			</MyPageShell>
		);
	}

	if (error || !profile) {
		return (
			<MyPageShell>
				<p className="py-12 text-center text-body-md text-secondary">
					프로필 정보를 불러오지 못했습니다.
				</p>
			</MyPageShell>
		);
	}

	return (
		<MyPageShell>
			<h1 className="mb-6 border-b border-outline-variant pb-4 text-headline-lg font-display text-on-surface">
				프로필 설정
			</h1>

			<div className="mb-8 flex items-center gap-4">
				<div className="relative">
					<Avatar src={profile.profileImageUrl ?? undefined} size={80} />
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
