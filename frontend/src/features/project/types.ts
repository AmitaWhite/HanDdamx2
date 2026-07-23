/** 백엔드 ProjectResponse (com.white.handdam.project.dto.response.ProjectResponse) 와 1:1 */

export interface ProjectCreatorSummary {
	creatorId: number;
	nickname: string;
	profileImageUrl: string | null;
}

export interface ProjectCategorySummary {
	categoryId: number;
	name: string;
}

export interface ProjectResponse {
	projectId: number;
	creator: ProjectCreatorSummary;
	category: ProjectCategorySummary;
	title: string;
	description: string | null;
	coverImageUrl: string | null;
	feedCount: number;
	isMine: boolean;
	createdAt: string;
	updatedAt: string;
}
