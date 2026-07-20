import { Link } from "react-router-dom";
import { paths } from "@/app/paths";
import { Avatar } from "@/components/ui/Avatar";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { findCreator, mockCreators } from "@/mocks/creators";
import { mockImg } from "@/mocks/helpers";
import { mockPosts } from "@/mocks/posts";

export function SubscribeFeedPage() {
	const subscribedCreators = mockCreators.slice(0, 5);

	return (
		<div className="container-page grid grid-cols-1 gap-gutter py-6 lg:grid-cols-[1fr_280px]">
			<div>
				<h1 className="mb-6 text-headline-lg font-display text-on-surface">구독 중인 작가의 새 게시물</h1>
				<div className="flex flex-col gap-gutter">
					{mockPosts.map((post) => {
						const creator = findCreator(post.creatorId);
						return (
							<Link key={post.id} to={paths.postDetail(post.id)}>
								<Card interactive className="overflow-hidden">
									<div className="flex items-center gap-3 p-5 pb-4">
										<Avatar src={mockImg(creator.avatarSeed, 80, 80)} size={40} />
										<div className="min-w-0">
											<p className="truncate text-label-md font-label-md text-on-surface">{creator.name}</p>
											<p className="truncate text-caption font-caption text-secondary">{creator.category}</p>
										</div>
										{post.isPaid && (
											<span className="ml-auto shrink-0 rounded bg-primary px-2 py-1 text-[10px] font-bold text-on-primary">
												유료
											</span>
										)}
									</div>
									<div className="aspect-[16/9] overflow-hidden">
										<img
											src={mockImg(post.imageSeed, 900, 500)}
											alt={post.title}
											className="h-full w-full object-cover"
										/>
									</div>
									{post.poll && (
										<div className="border-t border-outline-variant/50 p-5">
											<p className="mb-3 text-label-md font-label-md text-on-surface">{post.poll.question}</p>
											<div className="flex flex-col gap-2">
												{post.poll.options.map((opt) => (
													<div key={opt.label} className="relative overflow-hidden rounded bg-surface-container-low">
														<div className="h-8 bg-primary/15" style={{ width: `${opt.percentage}%` }} />
														<div className="absolute inset-0 flex items-center justify-between px-3 text-caption font-caption text-on-surface">
															<span>{opt.label}</span>
															<span>{opt.percentage}%</span>
														</div>
													</div>
												))}
											</div>
											<p className="mt-2 text-caption font-caption text-secondary">
												{post.poll.totalVotes}명 참여 · {post.poll.note}
											</p>
										</div>
									)}
									<div className="p-5">
										<div className="mb-2 flex items-center gap-4 text-caption font-caption text-secondary">
											<span className="flex items-center gap-1">
												<Icon name="favorite" className="text-[16px] text-primary" />
												{post.likeCount}
											</span>
											<span className="flex items-center gap-1">
												<Icon name="chat_bubble_outline" className="text-[16px]" />
												{post.commentCount}
											</span>
										</div>
										<p className="mb-1 text-body-md text-on-surface">{post.excerpt}</p>
										<p className="text-caption font-caption text-secondary">{post.createdAtLabel}</p>
									</div>
								</Card>
							</Link>
						);
					})}
				</div>
			</div>

			<aside className="hidden lg:block">
				<div className="sticky top-[96px] rounded-xl border border-outline-variant/50 bg-surface-container-lowest p-5">
					<h2 className="mb-4 text-label-md font-label-md text-on-surface">구독 중인 작가</h2>
					<div className="flex flex-col gap-3">
						{subscribedCreators.map((creator) => (
							<Link key={creator.id} to={paths.creator(creator.id)} className="flex items-center gap-3">
								<Avatar src={mockImg(creator.avatarSeed, 80, 80)} size={36} />
								<div className="min-w-0">
									<p className="truncate text-label-md font-label-md text-on-surface">{creator.name}</p>
									<p className="truncate text-caption font-caption text-secondary">{creator.category}</p>
								</div>
							</Link>
						))}
					</div>
					<Link to={paths.home} className="mt-4 block text-label-md font-label-md text-primary hover:underline">
						더 많은 작가 둘러보기
					</Link>
				</div>
			</aside>
		</div>
	);
}
