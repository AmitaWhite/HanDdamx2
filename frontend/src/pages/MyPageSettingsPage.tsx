import { useEffect, useState } from "react";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { getMyProfile } from "@/features/member/memberApi";
import type { MemberProfileResponse } from "@/features/member/types";
import { NicknameForm } from "@/components/member/NicknameForm";
import { PasswordForm } from "@/components/member/PasswordForm";

export function MyPageSettingsPage() {
	const [profile, setProfile] = useState<MemberProfileResponse | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState(false);

	useEffect(() => {
		let ignore = false;

		const fetchProfile = async () => {
			try {
				const data = await getMyProfile();

				if (!ignore) {
					setProfile(data);
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

			<NicknameForm initialNickname={profile.nickname} />
			<PasswordForm />

			<div className="mt-6">
				<LinkButton to={paths.mypage} variant="ghost">
					취소
				</LinkButton>
			</div>
		</MyPageShell>
	);
}