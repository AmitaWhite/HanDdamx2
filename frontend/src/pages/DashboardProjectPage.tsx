import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { cn } from "@/lib/cn";
import { mockImg } from "@/mocks/helpers";
import { mockPosts } from "@/mocks/posts";
import { findProject, PROJECT_STAGES } from "@/mocks/projects";

const STAGE_COLOR: Record<string, string> = {
	concept: "bg-secondary-container text-on-secondary-container",
	prep: "bg-surface-container-high text-secondary",
	production: "bg-primary-container text-on-primary-container",
	completion: "bg-tertiary-container text-on-tertiary-container",
};

export function DashboardProjectPage() {
	const { projectId = "" } = useParams();
	const project = findProject(projectId);
	const currentStageIndex = PROJECT_STAGES.findIndex((s) => s.id === project.currentStage);
	const posts = mockPosts.filter((p) => p.projectId === project.id);

	return (
		<div className="container-page py-8">
			<Link
				to={paths.dashboardProjects}
				className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				돌아가기
			</Link>

			<div className="mb-6 flex flex-wrap items-center justify-between gap-4">
				<div>
					<p className="text-caption font-caption uppercase tracking-wider text-secondary">프로젝트</p>
					<h1 className="text-headline-lg font-display text-on-surface">{project.title}</h1>
					<p className="mt-1 text-caption font-caption text-secondary">
						게시물 {project.postCount}개 · {project.periodLabel}
					</p>
				</div>
				<LinkButton to={paths.dashboardPostNew} state={{ projectId: project.id }}>
					<Icon name="add" className="text-[18px]" />
					새 포스트 작성
				</LinkButton>
			</div>

			<Card className="mb-8 p-6">
				<div className="flex items-center">
					{PROJECT_STAGES.map((stage, i) => (
						<div key={stage.id} className="flex flex-1 items-center last:flex-none">
							<div className="flex flex-col items-center gap-2">
								<span
									className={cn(
										"flex h-10 w-10 items-center justify-center rounded-full border-2 text-label-md font-label-md",
										i < currentStageIndex && "border-primary bg-primary text-on-primary",
										i === currentStageIndex && "border-primary bg-primary-container text-on-primary-container",
										i > currentStageIndex && "border-outline-variant text-secondary",
									)}
								>
									{i + 1}
								</span>
								<div className="text-center">
									<p className="text-caption font-caption text-on-surface">{stage.ko}</p>
									<p className="text-[10px] text-secondary">{stage.en}</p>
								</div>
							</div>
							{i < PROJECT_STAGES.length - 1 && (
								<div
									className={cn("mx-2 h-0.5 flex-1", i < currentStageIndex ? "bg-primary" : "bg-outline-variant")}
								/>
							)}
						</div>
					))}
				</div>
			</Card>

			<div className="flex flex-col gap-4">
				{posts.map((post) => {
					const stage = PROJECT_STAGES.find((s) => s.id === post.stage);
					return (
						<Link key={post.id} to={paths.postDetail(post.id)}>
							<Card interactive className="flex gap-4 p-4">
								<img
									src={mockImg(post.imageSeed, 160, 160)}
									alt={post.title}
									className="h-20 w-20 shrink-0 rounded-lg object-cover"
								/>
								<div className="min-w-0 flex-1">
									<div className="mb-1 flex items-center gap-2">
										{stage && (
											<span className={cn("rounded px-2 py-0.5 text-[10px] font-bold", STAGE_COLOR[stage.id])}>
												{stage.ko}
											</span>
										)}
										{post.isPaid && (
											<Chip active size="sm">
												유료
											</Chip>
										)}
									</div>
									<h3 className="truncate text-label-md font-label-md text-on-surface">{post.title}</h3>
									<p className="mt-1 truncate text-caption font-caption text-secondary">{post.excerpt}</p>
									<p className="mt-1 text-caption font-caption text-secondary">
										{post.createdAtLabel} · 댓글 {post.commentCount} · 투표 {post.voteCount ?? 0}
									</p>
								</div>
							</Card>
						</Link>
					);
				})}
				{posts.length === 0 && <p className="py-8 text-center text-body-md text-secondary">아직 게시물이 없어요.</p>}
			</div>

			{posts.length > 0 && (
				<div className="mt-6 text-center">
					<Button variant="secondary">게시물 더보기</Button>
				</div>
			)}
		</div>
	);
}
