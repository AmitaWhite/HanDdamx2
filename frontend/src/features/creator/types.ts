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
