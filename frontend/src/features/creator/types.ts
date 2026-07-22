/** 백엔드 CreatorProfileResponse 대응 */
export interface CreatorProfile {
  creatorId: number;
  memberId: number;
  nickname: string;
  profileImageUrl: string | null;
  coverImageUrl: string | null;
  representativeImageUrl: string | null;
  introduction: string | null;
  benefitsDescription: string | null;
  subscriptionPrice: number;
  subscriptionLevel: string | null;
  subscriptionStatus: string | null;
  subscriberCount: number;
  projectCount: number;
  feedCount: number;
  isMine: boolean;
}

/** 백엔드 ProjectResponse의 프론트 타입 */
export interface ProjectSummary {
  projectId: number;
  creator: {
    creatorId: number;
    nickname: string;
    profileImageUrl: string | null;
  };
  category: {
    categoryId: number;
    name: string;
  };
  title: string;
  description: string | null;
  coverImageUrl: string | null;
  feedCount: number;
  isMine: boolean;
  createdAt: string;
  updatedAt: string;
}

export type CreatorApplicationStatus = "PENDING" | "APPROVED" | "REJECTED";

/** 백엔드 CreatorApplicationResponse 대응 */
export interface CreatorApplication {
  id: number;
  memberId: number;
  status: CreatorApplicationStatus;
  introduction: string | null;
  representativeImageUrl: string | null;
  reviewedBy: number | null;
  rejectReason: string | null;
  appliedAt: string;
  reviewedAt: string | null;
}

/** 백엔드 Page<T> 대응 */
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // 현재 페이지 (0-based)
  size: number;
  last: boolean;
}

/** 백엔드 Visibility enum */
export type FeedVisibility = "PUBLIC" | "SUBSCRIBERS_ONLY" | "PAID_ONLY";

/** 백엔드 FeedSummaryResponse 대응 (프로젝트 내 피드 목록) */
export interface FeedSummary {
  id: number;
  projectId: number;
  title: string;
  content: string;
  visibility: FeedVisibility;
  likeCount: number;
  commentCount: number;
  createdAt: string;
  updatedAt: string;
  creator: {
    creatorId: number;
    nickname: string;
    profileImageUrl: string | null;
  };
  category: {
    categoryId: number;
    name: string;
  };
}

/** 백엔드 SliceResponse<T> 대응 (무한 스크롤용, ApiResponse 미사용) */
export interface SliceResponse<T> {
  content: T[];
  hasNext: boolean;
  page: number;
  size: number;
}
