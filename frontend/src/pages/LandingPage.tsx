import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Footer } from "@/components/nav/Footer";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";

const img = (seed: string, w = 800, h = 600) =>
	`https://picsum.photos/seed/${seed}/${w}/${h}`;

const creators = [
	{
		name: "박서연 · 자수공방",
		category: "자수",
		subs: "3,240",
		seed: "artisan1",
	},
	{
		name: "이도윤 · 도자기 스튜디오",
		category: "도자기",
		subs: "1,890",
		seed: "artisan2",
	},
	{
		name: "최민재 · 가죽공방 온",
		category: "가죽공예",
		subs: "2,510",
		seed: "artisan3",
	},
	{
		name: "한지우 · 원목가구 공작소",
		category: "목공",
		subs: "1,420",
		seed: "artisan4",
	},
];

const records = [
	{
		author: "김도예",
		title: "물레로 빚은 백자 달항아리",
		tag: "도자기",
		paid: false,
		seed: "rec1",
	},
	{
		author: "정가죽",
		title: "식물성 염색 카드지갑",
		tag: "가죽공예",
		paid: true,
		seed: "rec2",
	},
	{
		author: "윤유리",
		title: "토치로 만든 미니 유리병",
		tag: "유리공예",
		paid: true,
		seed: "rec3",
	},
	{
		author: "박목수",
		title: "도브테일 서랍 짜기",
		tag: "목공",
		paid: false,
		seed: "rec4",
	},
	{
		author: "이수연",
		title: "프랑스자수로 그린 들꽃 리스",
		tag: "자수",
		paid: true,
		seed: "rec5",
	},
	{
		author: "한글씨",
		title: "붓펜으로 쓴 사계절 문장",
		tag: "캘리그라피",
		paid: false,
		seed: "rec6",
	},
];

export function LandingPage() {
	return (
		<>
			{/* Hero */}
			<section className="container-page flex flex-col items-center gap-12 py-section-gap lg:flex-row">
				<div className="flex-1">
					<span className="mb-4 block text-label-md font-label-md font-bold uppercase tracking-wider text-primary">
						Craft Subscription
					</span>
					<h1 className="mb-6 text-headline-lg-mobile font-display leading-tight text-on-surface md:text-display-lg md:font-display">
						작가의 손끝에서
						<br />
						완성되기까지,{" "}
						<span className="relative inline-block">
							한 땀 한 땀
							<span className="absolute bottom-1 left-0 h-[4px] w-full -rotate-1 bg-primary/20" />
						</span>
					</h1>
					<p className="mb-8 max-w-lg text-body-lg leading-relaxed text-secondary">
						도자기, 목공, 자수, 가죽공예 — 공예 작가의 작업 과정을 가장
						가까이에서 기록하고 구독하세요. 창작의 모든 순간이 당신의 영감이
						됩니다.
					</p>
					<div className="flex flex-wrap gap-4">
						<Link to={paths.home}>
							<Button size="lg">작가 둘러보기</Button>
						</Link>
						<Link to={paths.login}>
							<Button size="lg" variant="secondary">
								로그인 후 구독 피드 보기
								<Icon name="arrow_forward" />
							</Button>
						</Link>
					</div>
				</div>
				<div className="w-full flex-1">
					<div className="aspect-[4/3] overflow-hidden rounded-2xl shadow-card-hover">
						<img
							src={img("hero-pottery", 900, 700)}
							alt="공방에서 작업 중인 공예 작가"
							className="h-full w-full object-cover"
						/>
					</div>
				</div>
			</section>

			{/* 인기 크리에이터 */}
			<section className="bg-surface-container-low py-section-gap">
				<div className="container-page">
					<div className="mb-12 flex items-end justify-between">
						<div>
							<h2 className="mb-2 text-headline-lg font-display text-on-surface">
								인기 크리에이터
							</h2>
							<p className="text-body-md text-secondary">
								지금 가장 주목받는 핸드메이드 작가들을 만나보세요.
							</p>
						</div>
						<Link
							to={paths.home}
							className="hidden items-center gap-1 text-label-md font-label-md font-bold text-primary hover:underline sm:flex"
						>
							전체보기 <Icon name="chevron_right" />
						</Link>
					</div>
					<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-4">
						{creators.map((c) => (
							<Card key={c.name} interactive className="p-8 text-center">
								<Avatar
									src={img(c.seed, 200, 200)}
									size={96}
									className="mx-auto mb-6 border-4 border-surface-container-high"
								/>
								<h3 className="mb-1 text-headline-md font-display text-on-surface">
									{c.name}
								</h3>
								<Chip className="mb-4">{c.category}</Chip>
								<p className="mb-6 text-body-md text-secondary">
									구독자 {c.subs}명
								</p>
								<Button variant="outline" fullWidth>
									구독하기
								</Button>
							</Card>
						))}
					</div>
				</div>
			</section>

			{/* 최근 작업 기록 */}
			<section className="container-page py-section-gap">
				<h2 className="mb-12 text-headline-lg font-display text-on-surface">
					최근 업로드된 작업 기록
				</h2>
				<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2 lg:grid-cols-3">
					{records.map((r) => (
						<Card key={r.title} interactive className="group overflow-hidden">
							<div className="relative aspect-square overflow-hidden">
								<img
									src={img(r.seed)}
									alt={r.title}
									className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
								/>
								{r.paid && (
									<span className="absolute right-4 top-4 rounded bg-primary px-2 py-1 text-[10px] font-bold text-on-primary">
										유료
									</span>
								)}
							</div>
							<div className="p-6">
								<div className="mb-4 flex items-center gap-3">
									<Avatar size={32} />
									<span className="text-label-md font-label-md text-on-surface">
										{r.author}
									</span>
								</div>
								<h4 className="mb-2 text-headline-md font-display text-on-surface">
									{r.title}
								</h4>
								<span className="text-caption font-caption text-secondary">
									#{r.tag}
								</span>
							</div>
						</Card>
					))}
				</div>
			</section>

			<Footer />
		</>
	);
}
