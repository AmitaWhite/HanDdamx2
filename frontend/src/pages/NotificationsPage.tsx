import { useNavigate } from "react-router-dom";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { useNotifications } from "@/features/notifications/NotificationsContext";
import { resolveNotificationHref } from "@/features/notifications/notificationRoutes";
import {
	NOTIFICATION_TYPE_ICON,
	type NotificationResponse,
} from "@/features/notifications/notificationsApi";
import { cn } from "@/lib/cn";
import { formatRelativeTime } from "@/lib/relativeTime";

export function NotificationsPage() {
	const {
		notifications: items,
		hasNext,
		loading,
		markAllRead,
		markOneRead,
		loadMore,
	} = useNotifications();
	const navigate = useNavigate();

	async function handleClick(n: NotificationResponse) {
		if (!n.isRead) void markOneRead(n.id);
		const href = await resolveNotificationHref(n);
		navigate(href);
	}

	return (
		<div className="container-page py-6">
			<Card className="mx-auto max-w-2xl overflow-hidden">
				<div className="flex items-center justify-between border-b border-outline-variant/50 p-5">
					<h1 className="text-headline-md font-display text-on-surface">
						알림
					</h1>
					<button
						type="button"
						onClick={markAllRead}
						className="text-label-md font-label-md text-primary hover:underline"
					>
						모두 읽음
					</button>
				</div>
				<div className="divide-y divide-outline-variant/30">
					{items.map((n) => (
						<button
							key={n.id}
							type="button"
							onClick={() => handleClick(n)}
							className={cn(
								"flex w-full items-start gap-3 p-5 text-left transition-colors hover:bg-surface-container-low",
								!n.isRead && "bg-primary/5",
							)}
						>
							<span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-surface-container text-secondary">
								<Icon
									name={NOTIFICATION_TYPE_ICON[n.type]}
									className="text-[20px]"
								/>
							</span>
							<div className="min-w-0 flex-1">
								<p
									className={cn(
										"text-body-md text-on-surface",
										!n.isRead && "font-bold",
									)}
								>
									{n.message}
								</p>
								<p
									className={cn(
										"mt-1 text-caption font-caption",
										!n.isRead ? "text-primary" : "text-secondary",
									)}
								>
									{formatRelativeTime(n.createdAt)}
								</p>
							</div>
						</button>
					))}
					{items.length === 0 && (
						<p className="p-8 text-center text-body-md text-secondary">
							알림이 없어요.
						</p>
					)}
				</div>
				{hasNext && (
					<div className="p-5 text-center">
						<button
							type="button"
							onClick={() => loadMore()}
							disabled={loading}
							className="text-label-md font-label-md text-secondary hover:text-primary disabled:opacity-50"
						>
							{loading ? "불러오는 중…" : "이전 알림 더보기"}
						</button>
					</div>
				)}
			</Card>
		</div>
	);
}
