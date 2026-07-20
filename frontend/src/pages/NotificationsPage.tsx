import { Link } from "react-router-dom";
import { Card } from "@/components/ui/Card";
import { Icon } from "@/components/ui/Icon";
import { useNotifications } from "@/features/notifications/NotificationsContext";
import { cn } from "@/lib/cn";
import { NOTIFICATION_TYPE_ICON } from "@/mocks/notifications";

export function NotificationsPage() {
	const { notifications: items, markAllRead } = useNotifications();

	return (
		<div className="container-page py-6">
			<Card className="mx-auto max-w-2xl overflow-hidden">
				<div className="flex items-center justify-between border-b border-outline-variant/50 p-5">
					<h1 className="text-headline-md font-display text-on-surface">알림</h1>
					<button type="button" onClick={markAllRead} className="text-label-md font-label-md text-primary hover:underline">
						모두 읽음
					</button>
				</div>
				<div className="divide-y divide-outline-variant/30">
					{items.map((n) => (
						<Link
							key={n.id}
							to={n.href}
							className={cn(
								"flex items-start gap-3 p-5 transition-colors hover:bg-surface-container-low",
								!n.isRead && "bg-primary/5",
							)}
						>
							<span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-surface-container text-secondary">
								<Icon name={NOTIFICATION_TYPE_ICON[n.type]} className="text-[20px]" />
							</span>
							<div className="min-w-0 flex-1">
								<p className={cn("text-body-md text-on-surface", !n.isRead && "font-bold")}>
									{n.message}
									{n.amount != null && <span className="ml-1 font-bold text-primary">{n.amount.toLocaleString()}원</span>}
								</p>
								<p className={cn("mt-1 text-caption font-caption", !n.isRead ? "text-primary" : "text-secondary")}>
									{n.createdAtLabel}
								</p>
							</div>
						</Link>
					))}
				</div>
				<div className="p-5 text-center">
					<button type="button" className="text-label-md font-label-md text-secondary hover:text-primary">
						이전 알림 더보기
					</button>
				</div>
			</Card>
		</div>
	);
}
