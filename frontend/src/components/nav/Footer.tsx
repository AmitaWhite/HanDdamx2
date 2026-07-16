/** 랜딩/공개 페이지용 푸터. */
export function Footer() {
	return (
		<footer className="border-t border-outline-variant bg-surface-container-highest">
			<div className="container-page flex flex-col justify-between gap-gutter py-section-gap md:flex-row">
				<div className="max-w-xs">
					<div className="mb-4 text-headline-md font-display font-bold text-on-surface">
						한땀한땀
					</div>
					<p className="text-body-md leading-relaxed text-on-tertiary-fixed-variant">
						작가의 과정이 가치가 되는 곳, 공예 작가와 팬을 잇는 구독
						플랫폼입니다.
					</p>
				</div>
				<div className="grid grid-cols-2 gap-12 sm:grid-cols-3">
					<FooterCol
						title="서비스"
						links={["소개", "구독", "크리에이터 센터"]}
					/>
					<FooterCol
						title="지원"
						links={["이용약관", "개인정보처리방침", "고객센터"]}
					/>
				</div>
			</div>
			<div className="container-page border-t border-outline-variant/30 py-8 text-center">
				<p className="text-caption font-caption text-secondary">
					© 2026 한땀한땀 (Handdam). All rights reserved.
				</p>
			</div>
		</footer>
	);
}

function FooterCol({ title, links }: { title: string; links: string[] }) {
	return (
		<div className="flex flex-col gap-4">
			<span className="text-label-md font-label-md font-bold text-on-surface">
				{title}
			</span>
			{links.map((link) => (
				<a
					key={link}
					href="#"
					className="text-body-md text-on-tertiary-fixed-variant transition-colors hover:text-primary"
				>
					{link}
				</a>
			))}
		</div>
	);
}
