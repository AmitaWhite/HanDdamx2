import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { EngagementBar } from "@/components/social/EngagementBar";
import { Icon } from "@/components/ui/Icon";
import { commentsFor } from "@/mocks/comments";
import { findCreator } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";
import { findPost } from "@/mocks/posts";

export function PostDetailPage() {
	const { postId = "" } = useParams();
	const post = findPost(postId);
	const creator = findCreator(post.creatorId);
	const [comments, setComments] = useState(commentsFor(post.id));
	const [liked, setLiked] = useState(false);
	const [selectedOption, setSelectedOption] = useState<string | null>(null);
	const [draft, setDraft] = useState("");

	function submitComment() {
		if (!draft.trim()) return;
		setComments((prev) => [
			...prev,
			{
				id: `local-${Date.now()}`,
				postId: post.id,
				authorName: "나",
				avatarSeed: "member1",
				isPaidSubscriber: false,
				body: draft.trim(),
				createdAtLabel: "방금",
			},
		]);
		setDraft("");
	}

	return (
		<div className="container-page grid grid-cols-1 gap-gutter py-6 lg:grid-cols-[1fr_400px]">
			<div>
				<Link
					to={paths.home}
					className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
				>
					<Icon name="arrow_back" className="text-[18px]" />
					돌아가기
				</Link>

				<div className="mb-4 flex items-center gap-3">
					<Link to={paths.creator(creator.id)}>
						<Avatar src={mockImg(creator.avatarSeed, 80, 80)} size={40} />
					</Link>
					<div className="min-w-0">
						<Link
							to={paths.creator(creator.id)}
							className="text-label-md font-label-md text-on-surface hover:text-primary"
						>
							{creator.name}
						</Link>
						<p className="text-caption font-caption text-secondary">{post.createdAtLabel}</p>
					</div>
					{post.isPaid && (
						<span className="ml-auto shrink-0 rounded bg-primary px-2 py-1 text-[10px] font-bold text-on-primary">
							유료 구독자 공개
						</span>
					)}
				</div>

				<div className="mb-4 aspect-[4/3] overflow-hidden rounded-xl">
					<img src={mockImg(post.imageSeed, 900, 700)} alt={post.title} className="h-full w-full object-cover" />
				</div>

				<EngagementBar
					likeCount={post.likeCount + (liked ? 1 : 0)}
					commentCount={comments.length}
					liked={liked}
					onToggleLike={() => setLiked((v) => !v)}
					className="mb-4"
				/>

				<h1 className="mb-2 text-headline-lg font-display text-on-surface">{post.title}</h1>
				<div className="mb-4 flex flex-col gap-3 text-body-md text-on-surface">
					{(post.body ?? [post.excerpt]).map((paragraph) => (
						<p key={paragraph}>{paragraph}</p>
					))}
				</div>
				{post.tags && (
					<div className="flex flex-wrap gap-2">
						{post.tags.map((t) => (
							<Chip key={t}>#{t}</Chip>
						))}
					</div>
				)}
			</div>

			<aside className="flex flex-col gap-6">
				{post.poll && (
					<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-5">
						<p className="mb-3 text-label-md font-label-md text-on-surface">{post.poll.question}</p>
						<div className="flex flex-col gap-2">
							{post.poll.options.map((opt) => (
								<button
									key={opt.label}
									type="button"
									onClick={() => setSelectedOption(opt.label)}
									className={
										"relative overflow-hidden rounded border text-left transition-colors " +
										(selectedOption === opt.label ? "border-primary" : "border-outline-variant")
									}
								>
									<div className="h-9 bg-primary/15" style={{ width: `${opt.percentage}%` }} />
									<div className="absolute inset-0 flex items-center justify-between px-3 text-caption font-caption text-on-surface">
										<span>{opt.label}</span>
										<span>{opt.percentage}%</span>
									</div>
								</button>
							))}
						</div>
						<p className="mt-3 text-caption font-caption text-secondary">
							{post.poll.totalVotes}명 참여 · {post.poll.note}
						</p>
					</div>
				)}

				<div className="rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-5">
					<div className="mb-4 flex items-center justify-between">
						<h2 className="text-label-md font-label-md text-on-surface">댓글 {comments.length}</h2>
						<span className="text-caption font-caption text-secondary">최신순</span>
					</div>
					<div className="mb-4 flex max-h-80 flex-col gap-4 overflow-y-auto">
						{comments.map((c) => (
							<div key={c.id} className="flex gap-3">
								<Avatar src={mockImg(c.avatarSeed, 80, 80)} size={28} />
								<div>
									<div className="flex items-center gap-2">
										<span className="text-label-md font-label-md text-on-surface">{c.authorName}</span>
										{c.isPaidSubscriber && (
											<span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] font-bold text-primary">
												구독자
											</span>
										)}
									</div>
									<p className="text-body-md text-on-surface">{c.body}</p>
									<p className="text-caption font-caption text-secondary">{c.createdAtLabel}</p>
								</div>
							</div>
						))}
					</div>
					<div className="flex items-center gap-2">
						<input
							value={draft}
							onChange={(e) => setDraft(e.target.value)}
							onKeyDown={(e) => e.key === "Enter" && submitComment()}
							placeholder="댓글을 입력하세요"
							className="h-10 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
						/>
						<Button size="sm" onClick={submitComment}>
							등록
						</Button>
					</div>
				</div>
			</aside>
		</div>
	);
}
