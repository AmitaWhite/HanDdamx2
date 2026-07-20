import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { MyPageShell } from "@/components/nav/MyPageShell";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Icon } from "@/components/ui/Icon";
import { findCreator } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";
import { mockProjects } from "@/mocks/projects";

/** 로그인 계정과 mock 크리에이터를 잇는 백엔드 매핑이 아직 없어 임시로 고정한 값. */
const MOCK_CREATOR_ID = "suyeon";

export function DashboardProjectsPage() {
	const creator = findCreator(MOCK_CREATOR_ID);
	const projects = mockProjects.filter((p) => p.creatorId === MOCK_CREATOR_ID);

	return (
		<MyPageShell>
			<div className="relative">
				<div className="mb-8">
					<p className="text-caption font-caption text-secondary">{creator.name}</p>
					<h1 className="text-headline-lg font-display text-on-surface">프로젝트</h1>
					<p className="mt-1 text-body-md text-secondary">진행 중인 작업을 프로젝트로 묶어 기록해보세요.</p>
				</div>

				<div className="grid grid-cols-1 gap-gutter sm:grid-cols-2">
					{projects.map((project) => (
						<Link key={project.id} to={paths.dashboardProject(project.id)}>
							<Card interactive className="overflow-hidden">
								<div className="aspect-[4/3] overflow-hidden">
									<img
										src={mockImg(project.thumbnailSeed, 500, 375)}
										alt={project.title}
										className="h-full w-full object-cover"
									/>
								</div>
								<div className="p-5">
									<div className="mb-2 flex items-center justify-between gap-2">
										<h3 className="text-headline-md font-display text-on-surface">{project.title}</h3>
										<Chip active={project.status === "ongoing"} size="sm" className="shrink-0">
											{project.status === "ongoing" ? "진행중" : "완료"}
										</Chip>
									</div>
									<p className="text-caption font-caption text-secondary">
										게시물 {project.postCount}개 · {project.metaLabel}
									</p>
								</div>
							</Card>
						</Link>
					))}
				</div>

				<button
					type="button"
					aria-label="새 프로젝트 만들기"
					className="fixed bottom-8 right-8 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-on-primary shadow-card-hover transition-colors hover:bg-primary-hover"
				>
					<Icon name="add" className="text-[28px]" />
				</button>
			</div>
		</MyPageShell>
	);
}
