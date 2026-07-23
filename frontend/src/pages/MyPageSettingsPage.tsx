import { useEffect, useState } from "react";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Card } from "@/components/ui/Card";
import { LinkButton } from "@/components/ui/LinkButton";
import { getMyProfile } from "@/features/member/memberApi";
import type { MemberProfileResponse } from "@/features/member/types";
import { NicknameForm } from "@/components/member/NicknameForm";
import { PasswordForm } from "@/components/member/PasswordForm";
import { ProfileImageForm } from "@/components/member/ProfileImageForm";

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

			<div className="flex flex-col gap-6">
				<Card className="p-6">
					<h2 className="mb-5 text-headline-sm font-display text-on-surface">기본 정보</h2>
					<ProfileImageForm profile={profile} onProfileUpdated={setProfile} />
					<NicknameForm initialNickname={profile.nickname} onProfileUpdated={setProfile} />
				</Card>

				{profile.oauthProvider === "NONE" && (
					<Card className="p-6">
						<h2 className="mb-5 text-headline-sm font-display text-on-surface">보안</h2>
						<PasswordForm />
					</Card>
				)}
			</div>

			<div className="mt-6">
				<LinkButton to={paths.mypage} variant="secondary">
					취소
				</LinkButton>
			</div>
		</MyPageShell>
	);
}