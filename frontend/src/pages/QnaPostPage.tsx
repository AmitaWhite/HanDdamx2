import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { EngagementBar } from "@/components/social/EngagementBar";
import { Icon } from "@/components/ui/Icon";
import { commentsFor } from "@/mocks/comments";
import { mockImg } from "@/mocks/helpers";
import { findQnaPost, mockQnaAnswers } from "@/mocks/qna";

export function QnaPostPage() {
	const { postId = "" } = useParams();
	const post = findQnaPost(postId);
	const answer = mockQnaAnswers[post.id];
	const [comments, setComments] = useState(commentsFor(post.id));
	const [liked, setLiked] = useState(false);
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
		<div className="container-page max-w-2xl py-6">
			<Link
				to={paths.creatorQna(post.creatorId)}
				className="mb-4 inline-flex items-center gap-1 text-label-md font-label-md text-secondary hover:text-primary"
			>
				<Icon name="arrow_back" className="text-[18px]" />
				Q&A 게시판
			</Link>

			<article>
				<div className="mb-3 flex items-center gap-2">
					<span
						className={
							"rounded px-2 py-1 text-[10px] font-bold " +
							(post.status === "answered" ? "bg-primary text-on-primary" : "bg-surface-container text-secondary")
						}
					>
						{post.status === "answered" ? "답변 완료" : "답변 대기"}
					</span>
					<span className="text-caption font-caption text-secondary">{post.category}</span>
				</div>
				<h1 className="mb-3 text-headline-lg font-display text-on-surface">{post.title}</h1>
				<div className="mb-5 flex items-center gap-2 text-caption font-caption text-secondary">
					<Avatar size={24} />
					{post.authorName} · {post.createdAtLabel}
				</div>
				<div className="mb-5 flex flex-col gap-3 text-body-md text-on-surface">
					{post.body.map((paragraph) => (
						<p key={paragraph}>{paragraph}</p>
					))}
				</div>
				{post.attachedImageSeeds && (
					<div className="mb-5 grid grid-cols-2 gap-3">
						{post.attachedImageSeeds.map((seed) => (
							<img
								key={seed}
								src={mockImg(seed, 400, 400)}
								alt="첨부 이미지"
								className="aspect-square rounded-lg object-cover"
							/>
						))}
					</div>
				)}
				<EngagementBar
					likeCount={24 + (liked ? 1 : 0)}
					commentCount={comments.length}
					liked={liked}
					onToggleLike={() => setLiked((v) => !v)}
					className="mb-6 border-b border-outline-variant/50 pb-5"
				/>
			</article>

			{answer && (
				<div className="mb-8 rounded-xl border border-primary/30 bg-primary/5 p-5">
					<div className="mb-3 flex items-center gap-2">
						<Icon name="workspace_premium" className="text-primary" />
						<Avatar src={mockImg(answer.avatarSeed, 80, 80)} size={28} />
						<span className="text-label-md font-label-md text-on-surface">{answer.creatorName}의 답변</span>
					</div>
					<div className="flex flex-col gap-2 text-body-md text-on-surface">
						{answer.body.map((paragraph) => (
							<p key={paragraph}>{paragraph}</p>
						))}
					</div>
					<p className="mt-3 text-caption font-caption text-secondary">{answer.answeredAtLabel}</p>
				</div>
			)}

			<h2 className="mb-4 text-label-md font-label-md text-on-surface">댓글 {comments.length}</h2>
			<div className="mb-5 flex flex-col gap-4">
				{comments.map((c) => (
					<div key={c.id} className="flex gap-3">
						<Avatar src={mockImg(c.avatarSeed, 80, 80)} size={32} />
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

			<div className="flex items-center gap-3">
				<Avatar size={32} />
				<input
					value={draft}
					onChange={(e) => setDraft(e.target.value)}
					onKeyDown={(e) => e.key === "Enter" && submitComment()}
					placeholder="댓글을 입력하세요"
					className="h-11 flex-1 rounded-full border border-outline-variant bg-surface-container-low px-4 text-body-md focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
				/>
				<Button size="sm" onClick={submitComment}>
					등록
				</Button>
			</div>
		</div>
	);
}
