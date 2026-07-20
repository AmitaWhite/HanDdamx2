import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { findCreator } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";
import { mockPosts } from "@/mocks/posts";
import { mockProjects } from "@/mocks/projects";

export function CreatorPage() {
	const { creatorId = "" } = useParams();
	const creator = findCreator(creatorId);
	const projects = mockProjects.filter((p) => p.creatorId === creator.id);
	const posts = mockPosts.filter((p) => p.creatorId === creator.id);

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
					<div className="flex shrink-0 gap-2">
						<LinkButton to={`${paths.chat}?creatorId=${creator.id}`} variant="secondary">
							메시지
						</LinkButton>
						<LinkButton to={paths.subscribeSelect(creator.id)}>구독하기</LinkButton>
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
					<div className="flex gap-gutter overflow-x-auto pb-2">
						{projects.map((project) => (
							<Link key={project.id} to={paths.dashboardProject(project.id)} className="w-48 shrink-0">
								<Card interactive className="overflow-hidden">
									<div className="aspect-square overflow-hidden">
										<img
											src={mockImg(project.thumbnailSeed, 400, 400)}
											alt={project.title}
											className="h-full w-full object-cover"
										/>
									</div>
									<p className="p-3 text-label-md font-label-md text-on-surface">{project.title}</p>
								</Card>
							</Link>
						))}
					</div>
				</div>
			)}

			<div className="container-page mt-10">
				<div className="mb-4 flex items-center gap-6 border-b border-outline-variant">
					<span className="border-b-2 border-primary pb-3 text-label-md font-label-md text-primary">게시물</span>
					{creator.hasQnaTab && (
						<Link
							to={paths.creatorQna(creator.id)}
							className="pb-3 text-label-md font-label-md text-secondary hover:text-primary"
						>
							Q&A
						</Link>
					)}
				</div>
				<div className="grid grid-cols-2 gap-gutter sm:grid-cols-3">
					{posts.map((post) => (
						<Link key={post.id} to={paths.postDetail(post.id)}>
							<Card interactive className="overflow-hidden">
								<div className="aspect-square overflow-hidden">
									<img
										src={mockImg(post.imageSeed, 400, 400)}
										alt={post.title}
										className="h-full w-full object-cover"
									/>
								</div>
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
							</Card>
						</Link>
					))}
					{posts.length === 0 && (
						<p className="col-span-full py-8 text-center text-body-md text-secondary">아직 게시물이 없어요.</p>
					)}
				</div>
			</div>
		</div>
	);
}
