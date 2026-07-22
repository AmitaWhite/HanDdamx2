import { useEffect, useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { CreatorQnaList } from "@/components/creator/CreatorQnaList";
import { PostCard } from "@/components/social/PostCard";
import { SubscriptionPlanPanel } from "@/components/subscription/SubscriptionPlanPanel";
import { Avatar } from "@/components/ui/Avatar";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { LinkButton } from "@/components/ui/LinkButton";
import { type BoardPostType, getPremiumBoardPosts } from "@/features/board/boardApi";
import { getCreatorProfile, getCreatorProjects } from "@/features/creator/creatorApi";
import type { CreatorProfile, ProjectSummary } from "@/features/creator/types";
import { getCreatorFeeds } from "@/features/feed/feedApi";
import type { FeedSummaryResponse } from "@/features/feed/types";
import { getProjectFeeds } from "@/features/project/projectApi";
import { ApiError } from "@/lib/api";
import { cn } from "@/lib/cn";
import { useInfiniteScroll } from "@/lib/useInfiniteScroll";
import { type MockQnaPost, type QnaCategory } from "@/mocks/qna";

const PROJECT_SCROLL_STEP = 220;
const QNA_PREVIEW_SIZE = 5;

const TYPE_TO_CATEGORY: Record<BoardPostType, QnaCategory> = {
  QUESTION: "제작 질문",
  FEEDBACK: "작품 피드백",
  CONTENT_SUGGESTION: "콘텐츠 제안",
  MATERIAL: "재료 추천",
  GENERAL: "일반 소통",
};

function toNumericCreatorId(value: string): number | null {
  if (!value) return null;
  const n = Number(value);
  return Number.isInteger(n) && n > 0 ? n : null;
}

function toRelativeLabel(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  if (Number.isNaN(diffMs) || diffMs < 0) return "방금";
  const minutes = Math.floor(diffMs / 60_000);
  if (minutes < 1) return "방금";
  if (minutes < 60) return `${minutes}분 전`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}일 전`;
  return `${Math.floor(days / 7)}주 전`;
}

export function CreatorPage() {
  const { creatorId = "" } = useParams();
  const numericCreatorId = toNumericCreatorId(creatorId);

  const [creator, setCreator] = useState<CreatorProfile | null>(null);
  const [projects, setProjects] = useState<ProjectSummary[]>([]);
  const [creatorLoading, setCreatorLoading] = useState(true);
  const [creatorError, setCreatorError] = useState<string | null>(null);

  const [activeTab, setActiveTab] = useState<"posts" | "qna">("posts");
  const [selectedProjectId, setSelectedProjectId] = useState<number | null>(null);
  const projectsRef = useRef<HTMLDivElement>(null);

  const [remoteQna, setRemoteQna] = useState<MockQnaPost[]>([]);
  const [qnaLoading, setQnaLoading] = useState(false);
  const [qnaError, setQnaError] = useState<string | null>(null);

  const [posts, setPosts] = useState<FeedSummaryResponse[]>([]);
  const [postsLoading, setPostsLoading] = useState(true);
  const [postsError, setPostsError] = useState<string | null>(null);
  const [postsPage, setPostsPage] = useState(0);
  const [postsHasNext, setPostsHasNext] = useState(false);
  const [postsLoadingMore, setPostsLoadingMore] = useState(false);
  const postsLoadMoreInFlightRef = useRef(false);

  useEffect(() => {
    if (!numericCreatorId) return;
    let cancelled = false;
    setCreatorLoading(true);
    setCreatorError(null);
    Promise.all([getCreatorProfile(numericCreatorId), getCreatorProjects(numericCreatorId)])
      .then(([profile, projs]) => {
        if (cancelled) return;
        setCreator(profile);
        setProjects(projs);
      })
      .catch((e) => {
        if (cancelled) return;
        setCreatorError(e instanceof Error ? e.message : "크리에이터 정보를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!cancelled) setCreatorLoading(false);
      });
    return () => {
      cancelled = true;
      };
  }, [numericCreatorId]);

  useEffect(() => {
    if (numericCreatorId === null) return;

    let cancelled = false;
    setQnaLoading(true);
    setQnaError(null);

    getPremiumBoardPosts({
      creatorId: numericCreatorId,
      page: 0,
      size: QNA_PREVIEW_SIZE,
    })
      .then((page) => {
        if (cancelled) return;
        setRemoteQna(
          page.content.map((post) => ({
            id: String(post.id),
            creatorId: String(post.creatorId),
            title: post.title,
            status: post.status === "ANSWERED" ? "answered" : "pending",
            category: TYPE_TO_CATEGORY[post.type],
            authorName: post.memberNickname,
            commentCount: 0,
            createdAtLabel: toRelativeLabel(post.createdAt),
            body: [post.content],
          })),
        );
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setRemoteQna([]);
        setQnaError(err instanceof ApiError ? err.message : "Q&A를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!cancelled) setQnaLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [numericCreatorId]);

  // 프로젝트를 선택하지 않았으면 크리에이터 전체 피드(LYJ-009), 선택했으면 그 프로젝트의 피드만(LYJ-011)
  useEffect(() => {
    if (numericCreatorId === null) return;

    let cancelled = false;
    setPostsLoading(true);
    setPostsError(null);

    const request =
      selectedProjectId != null
        ? getProjectFeeds(selectedProjectId)
        : getCreatorFeeds(numericCreatorId);

    request
      .then((res) => {
        if (cancelled) return;
        setPosts(res.content);
        setPostsHasNext(res.hasNext);
        setPostsPage(0);
      })
      .catch((err) => {
        if (cancelled) return;
        setPosts([]);
        setPostsError(err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!cancelled) setPostsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [numericCreatorId, selectedProjectId]);

  async function loadMorePosts() {
    if (postsLoadMoreInFlightRef.current || !postsHasNext || numericCreatorId === null) return;
    postsLoadMoreInFlightRef.current = true;
    setPostsLoadingMore(true);
    try {
      const nextPage = postsPage + 1;
      const res =
        selectedProjectId != null
          ? await getProjectFeeds(selectedProjectId, nextPage)
          : await getCreatorFeeds(numericCreatorId, nextPage);
      setPosts((prev) => [...prev, ...res.content]);
      setPostsHasNext(res.hasNext);
      setPostsPage(nextPage);
    } catch (err) {
      setPostsError(err instanceof ApiError ? err.message : "게시물을 불러오지 못했습니다.");
    } finally {
      postsLoadMoreInFlightRef.current = false;
      setPostsLoadingMore(false);
    }
  }

  const postsSentinelRef = useInfiniteScroll(loadMorePosts, postsHasNext);

  if (creatorLoading) {
    return <div className="flex min-h-[60vh] items-center justify-center text-secondary">불러오는 중...</div>;
  }
  if (creatorError || !creator) {
    return <div className="flex min-h-[60vh] items-center justify-center text-secondary">{creatorError ?? "크리에이터를 찾을 수 없습니다."}</div>;
  }

  function scrollProjects(direction: "left" | "right") {
    projectsRef.current?.scrollBy({
      left: direction === "left" ? -PROJECT_SCROLL_STEP : PROJECT_SCROLL_STEP,
      behavior: "smooth",
    });
  }

  const postsGrid = (
    <div className="grid grid-cols-2 gap-gutter sm:grid-cols-3">
      {postsError && (
        <p className="col-span-full py-8 text-center text-body-md text-secondary">{postsError}</p>
      )}
      {!postsError &&
        posts.map((post) => (
          <PostCard key={post.id} href={paths.postDetail(post.id)} imageSeed={`feed-${post.id}`} imageAlt={post.title}>
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
      {!postsError && postsLoading && (
        <p className="col-span-full py-8 text-center text-body-md text-secondary">불러오는 중…</p>
      )}
      {!postsError && !postsLoading && posts.length === 0 && (
        <p className="col-span-full py-8 text-center text-body-md text-secondary">아직 게시물이 없어요.</p>
      )}
      {postsHasNext && <div ref={postsSentinelRef} className="col-span-full h-1" />}
      {postsLoadingMore && (
        <p className="col-span-full py-4 text-center text-body-md text-secondary">불러오는 중…</p>
      )}
      {!postsError && !postsLoading && !postsHasNext && posts.length > 0 && (
        <p className="col-span-full py-4 text-center text-caption font-caption text-secondary">마지막 게시물입니다</p>
      )}
    </div>
  );

  const qnaPanel = (
    <div>
      {qnaError && (
        <div className="mb-4 rounded-xl border border-primary/30 bg-primary/5 p-4 text-body-md text-primary">
          {qnaError}
        </div>
      )}
      {qnaLoading ? (
        <p className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-8 text-center text-body-md text-secondary">
          불러오는 중…
        </p>
      ) : (
        <CreatorQnaList qnaPosts={remoteQna} />
      )}
      <div className="mt-4 text-center">
        <Link to={paths.creatorQna(creator.memberId)} className="text-label-md font-label-md text-primary hover:underline">
          Q&A 게시판 전체보기 →
        </Link>
      </div>
    </div>
  );

  return (
    <div className="pb-16">
      <div className="h-[240px] w-full overflow-hidden sm:h-[320px]">
        {creator.coverImageUrl ? (
          <img src={creator.coverImageUrl} alt="" className="h-full w-full object-cover" />
        ) : (
          <div className="h-full w-full bg-surface-container-low" />
        )}
      </div>
      <div className="container-page relative -mt-16">
        <Card className="flex flex-col items-center gap-4 p-8 text-center sm:flex-row sm:text-left">
          <Avatar src={creator.profileImageUrl ?? undefined} size={96} className="border-4 border-surface-container-lowest" />
          <div className="min-w-0 flex-1">
            <h1 className="text-headline-md font-display text-on-surface">{creator.nickname}</h1>
            <p className="mt-1 text-caption font-caption text-secondary">
              구독자 {creator.subscriberCount.toLocaleString()}명 · 게시물 {creator.feedCount}개
            </p>
            {creator.introduction && (
              <p className="mt-3 text-body-md text-on-surface">{creator.introduction}</p>
            )}
          </div>
          {!creator.isMine && (
            <div className="flex shrink-0 flex-wrap justify-center gap-2">
              <LinkButton to={`${paths.chat}?creatorId=${creator.memberId}`} variant="secondary">
                메시지
              </LinkButton>
            </div>
          )}
        </Card>
      </div>

      <div className="container-page mt-6">
        <SubscriptionPlanPanel
          creatorId={creator.memberId}
          creatorNickname={creator.nickname}
          isOwnCreator={creator.isMine}
        />
      </div>

      {projects.length > 0 && (
        <div className="container-page mt-10">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-headline-md font-display text-on-surface">프로젝트 · 최신순</h2>
            {creator.isMine && (
              <Link to={paths.dashboardProjects} className="text-label-md font-label-md text-primary hover:underline">
                프로젝트별로 보기
              </Link>
            )}
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
                const isSelected = project.projectId === selectedProjectId;
                return (
                  <div key={project.projectId} className="relative w-48 shrink-0">
                    <Card interactive className={cn("overflow-hidden", isSelected && "ring-2 ring-primary")}>
                      <button
                        type="button"
                        aria-pressed={isSelected}
                        onClick={() => setSelectedProjectId(isSelected ? null : project.projectId)}
                        className="block w-full text-left"
                      >
                        <div className="aspect-square overflow-hidden bg-surface-container-low">
                          {project.coverImageUrl && (
                            <img src={project.coverImageUrl} alt={project.title} className="h-full w-full object-cover" />
                          )}
                        </div>
                        <p className="p-3 text-label-md font-label-md text-on-surface">{project.title}</p>
                      </button>
                    </Card>
                    {isSelected && (
                      <Link
                        to={paths.dashboardProject(project.projectId)}
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
        <div className="mb-4 flex items-center justify-center gap-6 border-b border-outline-variant">
          <button
            type="button"
            onClick={() => setActiveTab("posts")}
            className={cn(
              "pb-3 text-label-md font-label-md transition-colors",
              activeTab === "posts" ? "border-b-2 border-primary text-primary" : "text-secondary hover:text-primary",
            )}
          >
            게시물
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("qna")}
            className={cn(
              "pb-3 text-label-md font-label-md transition-colors",
              activeTab === "qna" ? "border-b-2 border-primary text-primary" : "text-secondary hover:text-primary",
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
      </div>
    </div>
  );
}
