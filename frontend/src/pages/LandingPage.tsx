import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Footer } from "@/components/nav/Footer";
import { PostCard } from "@/components/social/PostCard";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import { getPublicFeeds } from "@/features/feed/feedApi";
import type { FeedSummaryResponse } from "@/features/feed/types";
// 실험(lab): 히어로 물방울 글래스모피즘. 실험 종료 시 이 import 와 아래 사용처를 원복.
import { WaterHero } from "@/lab/WaterHero";
import { truncateText } from "@/lib/text";
import { mockCreators } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";

const featuredCreators = mockCreators.filter((c) =>
	["seoyeon", "doyoon", "minjae", "jiwoo"].includes(c.id),
);

export function LandingPage() {
	const { isAuthenticated } = useAuth();

	const [recentRecords, setRecentRecords] = useState<FeedSummaryResponse[]>([]);

	useEffect(() => {
		getPublicFeeds()
			.then((res) => setRecentRecords(res.content.slice(0, 6)))
			.catch(() => {
				// 랜딩 페이지 미리보기는 부가 기능 — 실패해도 섹션만 비우고 나머지는 그대로 노출
			});
	}, []);

	return (
		<>
			{/* Hero */}
			<section className="container-page flex flex-col items-center gap-12 py-section-gap lg:flex-row">
				<div className="min-w-0 flex-1">
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
						<LinkButton to={paths.home} size="lg">
							작가 둘러보기
						</LinkButton>
						{!isAuthenticated && (
							<LinkButton to={paths.login} size="lg" variant="secondary">
								로그인 후 구독 피드 보기
								<Icon name="arrow_forward" />
							</LinkButton>
						)}
					</div>
				</div>
				<div className="w-full flex-1">
					<div className="aspect-[4/3] [perspective:1200px]">
						<WaterHero
							src={mockImg("hero-pottery", 900, 700)}
							alt="공방에서 작업 중인 공예 작가"
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
						{featuredCreators.map((c) => (
							<Card key={c.id} interactive className="min-w-0 p-8 text-center">
								<Avatar
									fallbackText={c.name}
									size={96}
									className="mx-auto mb-6 border-4 border-surface-container-high"
								/>
								<h3 className="mb-1 text-headline-md font-display text-on-surface">
									{c.name}
								</h3>
								<Chip className="mb-4">{c.category}</Chip>
								<p className="mb-6 text-body-md text-secondary">
									구독자 {c.subscriberCount.toLocaleString()}명
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
					{recentRecords.map((post) => (
						<PostCard
							key={post.id}
							href={paths.postDetail(post.id)}
							imageSeed={`feed-${post.id}`}
							imageAlt={post.title}
							thumbnailUrl={post.thumbnailUrl}
							thumbnailType={post.thumbnailType}
						>
							<div className="p-6">
								<div className="mb-4 flex items-center gap-3">
									<Avatar
										src={post.creator.profileImageUrl ?? undefined}
										fallbackText={post.creator.nickname}
										size={32}
									/>
									<span className="text-label-md font-label-md text-on-surface">
										{post.creator.nickname}
									</span>
								</div>
								<h4 className="mb-2 text-headline-md font-display text-on-surface">
									{truncateText(post.title, 13)}
								</h4>
								<span className="text-caption font-caption text-secondary">
									#{post.category.name}
								</span>
							</div>
						</PostCard>
					))}
				</div>
			</section>

			<Footer />
		</>
	);
}
