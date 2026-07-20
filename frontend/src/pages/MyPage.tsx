import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Avatar } from "@/components/ui/Avatar";
import { LinkButton } from "@/components/ui/LinkButton";
import { mockImg } from "@/mocks/helpers";
import { mockMember } from "@/mocks/member";

export function MyPage() {
	return (
		<MyPageShell>
			<h1 className="mb-6 border-b border-outline-variant pb-4 text-headline-lg font-display text-on-surface">프로필</h1>

			<div className="mb-8 flex flex-col items-center gap-4 rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-8 text-center sm:flex-row sm:text-left">
				<Avatar src={mockImg(mockMember.avatarSeed, 160, 160)} size={80} />
				<div className="min-w-0 flex-1">
					<p className="text-display-lg font-display text-on-surface">{mockMember.nickname}</p>
				</div>
				<LinkButton to={paths.mypageSettings} variant="secondary">
					프로필 수정
				</LinkButton>
			</div>

			<div className="grid grid-cols-2 gap-gutter">
				<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-6">
					<p className="text-headline-lg font-display text-primary">{mockMember.subscribedCreatorCount}</p>
					<p className="mt-1 text-body-md text-secondary">구독 중인 크리에이터</p>
				</div>
				<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-6">
					<p className="text-headline-lg font-display text-primary">{mockMember.commentCount}</p>
					<p className="mt-1 text-body-md text-secondary">작성한 댓글</p>
				</div>
			</div>
		</MyPageShell>
	);
}
