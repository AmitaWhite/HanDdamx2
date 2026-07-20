import { useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { CreatorQnaList } from "@/components/creator/CreatorQnaList";
import { PostCard } from "@/components/social/PostCard";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { useAuth } from "@/features/auth/AuthContext";
import { useSubscription } from "@/features/subscription/SubscriptionContext";
import { cn } from "@/lib/cn";
import { findCreator } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";
import { mockPosts } from "@/mocks/posts";
import { mockProjects } from "@/mocks/projects";
import { mockQnaPosts } from "@/mocks/qna";

const PROJECT_SCROLL_STEP = 220;

export function CreatorPage() {
	const { creatorId = "" } = useParams();
	const creator = findCreator(creatorId);
	const projects = mockProjects.filter((p) => p.creatorId === creator.id);
	const posts = mockPosts.filter((p) => p.creatorId === creator.id);
	const qnaPosts = mockQnaPosts.filter((q) => q.creatorId === creator.id);

	const { isAuthenticated } = useAuth();
	const navigate = useNavigate();
	const { isFreeSubscribed, subscribeFree, unsubscribe } = useSubscription();
	const subscribed = isFreeSubscribed(creator.id);

	const [activeTab, setActiveTab] = useState<"posts" | "qna">("posts");
	const [selectedProjectId, setSelectedProjectId] = useState<string | null>(null);
	const projectsRef = useRef<HTMLDivElement>(null);

	function handleToggleSubscribe() {
		if (!isAuthenticated) {
			navigate(paths.login);
			return;
		}
		if (subscribed) unsubscribe(creator.id);
		else subscribeFree(creator.id);
	}

	function scrollProjects(direction: "left" | "right") {
		projectsRef.current?.scrollBy({
			left: direction === "left" ? -PROJECT_SCROLL_STEP : PROJECT_SCROLL_STEP,
			behavior: "smooth",
		});
	}

	const postsGrid = (
		<div className="grid grid-cols-2 gap-gutter sm:grid-cols-3">
			{posts.map((post) => (
				<PostCard key={post.id} href={paths.postDetail(post.id)} imageSeed={post.imageSeed} imageAlt={post.title}>
					<div className="p-4">
						<h3 className="mb-1 truncate text-label-md font-label-md text-on-surface">{post.title}</h3>
						<div className="flex items-center gap-3 text-caption font-caption text-secondary">
							<span className="flex items-center gap-1">
								<Icon name="favorite" className="text-[14px]" />
								{post.likeCount}
							</span>
							<span className="flex items-center gap-1">
								<Icon name="chat_bubble_outline" className="text-[14px]" />
								{post.commentCount}
							</span>
						</div>
					</div>
				</PostCard>
			))}
			{posts.length === 0 && <p className="col-span-full py-8 text-center text-body-md text-secondary">아직 게시물이 없어요.</p>}
		</div>
	);

	const qnaPanel = (
		<div>
			<CreatorQnaList qnaPosts={qnaPosts.slice(0, 5)} />
			<div className="mt-4 text-center">
				<Link
					to={paths.creatorQna(creator.id)}
					className="text-label-md font-label-md text-primary hover:underline"
				>
					Q&A 게시판 전체보기 →
				</Link>
			</div>
		</div>
	);

	return (
		<div className="pb-16">
			<div className="h-[240px] w-full overflow-hidden sm:h-[320px]">
				<img src={mockImg(creator.coverSeed, 1600, 500)} alt="" className="h-full w-full object-cover" />
			</div>
			<div className="container-page relative -mt-16">
				<Card className="flex flex-col items-center gap-4 p-8 text-center sm:flex-row sm:text-left">
					<Avatar
						src={mockImg(creator.avatarSeed, 200, 200)}
						size={96}
						className="border-4 border-surface-container-lowest"
					/>
					<div className="min-w-0 flex-1">
						<h1 className="text-headline-md font-display text-on-surface">{creator.name}</h1>
						<p className="text-body-md text-secondary">{creator.category}</p>
						<p className="mt-1 text-caption font-caption text-secondary">
							구독자 {creator.subscriberCount.toLocaleString()}명 · 게시물 {creator.postCount}개
						</p>
						<p className="mt-3 text-body-md text-on-surface">{creator.bio}</p>
					</div>
					<div className="flex shrink-0 flex-wrap justify-center gap-2">
						<LinkButton to={`${paths.chat}?creatorId=${creator.id}`} variant="secondary">
							메시지
						</LinkButton>
						<Button variant={subscribed ? "secondary" : "primary"} onClick={handleToggleSubscribe}>
							{subscribed ? "구독 중" : "구독하기"}
						</Button>
						{subscribed && (
							<LinkButton to={paths.subscribeSelect(creator.id)} state={{ mode: "support" }}>
								후원하기
							</LinkButton>
						)}
					</div>
				</Card>
			</div>

			{projects.length > 0 && (
				<div className="container-page mt-10">
					<div className="mb-4 flex items-center justify-between">
						<h2 className="text-headline-md font-display text-on-surface">프로젝트 · 최신순</h2>
						<Link
							to={paths.dashboardProjects}
							className="text-label-md font-label-md text-primary hover:underline"
						>
							프로젝트별로 보기
						</Link>
					</div>
					<div className="group relative">
						<button
							type="button"
							aria-label="이전 프로젝트"
							onClick={() => scrollProjects("left")}
							className="absolute left-0 top-[80px] z-10 hidden h-9 w-9 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-surface-container-lowest text-on-surface opacity-0 shadow-card-hover transition-opacity group-hover:flex group-hover:opacity-100"
						>
							<Icon name="chevron_left" />
						</button>

						<div ref={projectsRef} className="flex gap-gutter overflow-x-auto scroll-smooth pb-2">
							{projects.map((project) => {
								const isSelected = project.id === selectedProjectId;
								return (
									<div key={project.id} className="relative w-48 shrink-0">
										<Card interactive className={cn("overflow-hidden", isSelected && "ring-2 ring-primary")}>
											<button
												type="button"
												aria-pressed={isSelected}
												onClick={() => setSelectedProjectId(isSelected ? null : project.id)}
												className="block w-full text-left"
											>
												<div className="aspect-square overflow-hidden">
													<img
														src={mockImg(project.thumbnailSeed, 400, 400)}
														alt={project.title}
														className="h-full w-full object-cover"
													/>
												</div>
												<p className="p-3 text-label-md font-label-md text-on-surface">{project.title}</p>
											</button>
										</Card>
										{isSelected && (
											<Link
												to={paths.dashboardProject(project.id)}
												className="absolute inset-x-3 bottom-14 flex items-center justify-center gap-1 rounded-full bg-primary py-2 text-label-md font-label-md text-on-primary shadow-card-hover hover:bg-primary-hover"
											>
												보기
												<Icon name="arrow_forward" className="text-[16px]" />
											</Link>
										)}
									</div>
								);
							})}
						</div>

						<button
							type="button"
							aria-label="다음 프로젝트"
							onClick={() => scrollProjects("right")}
							className="absolute right-0 top-[80px] z-10 hidden h-9 w-9 -translate-y-1/2 translate-x-1/2 items-center justify-center rounded-full bg-surface-container-lowest text-on-surface opacity-0 shadow-card-hover transition-opacity group-hover:flex group-hover:opacity-100"
						>
							<Icon name="chevron_right" />
						</button>
					</div>
				</div>
			)}

			<div className="container-page mt-10">
				{creator.hasQnaTab ? (
					<>
						<div className="mb-4 flex items-center justify-center gap-6 border-b border-outline-variant">
							<button
								type="button"
								onClick={() => setActiveTab("posts")}
								className={cn(
									"pb-3 text-label-md font-label-md transition-colors",
									activeTab === "posts"
										? "border-b-2 border-primary text-primary"
										: "text-secondary hover:text-primary",
								)}
							>
								게시물
							</button>
							<button
								type="button"
								onClick={() => setActiveTab("qna")}
								className={cn(
									"pb-3 text-label-md font-label-md transition-colors",
									activeTab === "qna"
										? "border-b-2 border-primary text-primary"
										: "text-secondary hover:text-primary",
								)}
							>
								유료 Q&A
							</button>
						</div>
						<div className="overflow-hidden">
							<div
								className="flex w-[200%] transition-transform duration-300 ease-out"
								style={{ transform: activeTab === "posts" ? "translateX(0%)" : "translateX(-50%)" }}
							>
								<div
									className="w-1/2 pr-0 sm:pr-3"
									aria-hidden={activeTab !== "posts"}
									{...({ inert: activeTab !== "posts" ? "" : undefined } as Record<string, string | undefined>)}
								>
									{postsGrid}
								</div>
								<div
									className="w-1/2 pl-0 sm:pl-3"
									aria-hidden={activeTab !== "qna"}
									{...({ inert: activeTab !== "qna" ? "" : undefined } as Record<string, string | undefined>)}
								>
									{qnaPanel}
								</div>
							</div>
						</div>
					</>
				) : (
					<>
						<div className="mb-4 border-b border-outline-variant">
							<span className="inline-block border-b-2 border-primary pb-3 text-label-md font-label-md text-primary">
								게시물
							</span>
						</div>
						{postsGrid}
					</>
				)}
			</div>
		</div>
	);
}
