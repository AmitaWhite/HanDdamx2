import { Icon } from "@/components/ui/Icon";

/**
 * 아직 stitch 시안에서 컴포넌트로 이관하지 않은 화면용 임시 플레이스홀더.
 * 라우팅/레이아웃이 동작하는지 확인하는 용도. 이관 완료 시 실제 페이지로 교체.
 */
export function PagePlaceholder({
	title,
	source,
}: {
	title: string;
	source?: string;
}) {
	return (
		<div className="container-page flex min-h-[60vh] flex-col items-center justify-center py-section-gap text-center">
			<div className="mb-6 flex h-16 w-16 items-center justify-center rounded-full bg-surface-container text-secondary">
				<Icon name="construction" className="text-[32px]" />
			</div>
			<h1 className="mb-2 text-headline-lg font-display text-on-surface">
				{title}
			</h1>
			<p className="text-body-md text-secondary">
				이 화면은 이관 예정입니다{source ? ` (원본: ${source})` : ""}.
			</p>
		</div>
	);
}
