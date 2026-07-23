/** 백엔드 CreatorSearchResponse (com.white.handdam.search.dto.response.CreatorSearchResponse) 와 1:1 */
export interface CreatorSearchResponse {
	/** Member.id — 크리에이터 공개 프로필 라우트(/creators/:creatorId)의 id */
	creatorId: number;
	nickname: string;
	profileImageUrl: string | null;
	introduction: string | null;
}
