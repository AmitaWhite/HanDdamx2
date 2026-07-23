import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { paths } from "@/app/paths";
import { Icon } from "@/components/ui/Icon";
import { useNotifications } from "@/features/notifications/NotificationsContext";
import { resolveNotificationHref } from "@/features/notifications/notificationRoutes";
import {
	NOTIFICATION_TYPE_ICON,
	type NotificationResponse,
} from "@/features/notifications/notificationsApi";
import { cn } from "@/lib/cn";
import { formatRelativeTime } from "@/lib/relativeTime";

const PREVIEW_COUNT = 5;

/** 헤더 벨 아이콘 클릭 시 열리는 알림 드롭다운. 전체 알림 페이지와 읽음 상태를 공유. */
export function NotificationDropdown() {
	const { notifications, unreadCount, markAllRead, markOneRead } =
		useNotifications();
	const navigate = useNavigate();
	const [open, setOpen] = useState(false);
	const rootRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		function onPointerDown(e: PointerEvent) {
			if (rootRef.current && !rootRef.current.contains(e.target as Node))
				setOpen(false);
		}
		function onKeyDown(e: KeyboardEvent) {
			if (e.key === "Escape") setOpen(false);
		}
		document.addEventListener("pointerdown", onPointerDown);
		document.addEventListener("keydown", onKeyDown);
		return () => {
			document.removeEventListener("pointerdown", onPointerDown);
			document.removeEventListener("keydown", onKeyDown);
		};
	}, []);

	async function handleNotificationClick(n: NotificationResponse) {
		setOpen(false);
		if (!n.isRead) void markOneRead(n.id);
		const href = await resolveNotificationHref(n);
		navigate(href);
	}

	const preview = notifications.slice(0, PREVIEW_COUNT);

	return (
		<div ref={rootRef} className="relative">
			<button
				type="button"
				aria-label="알림"
				aria-expanded={open}
				onClick={() => setOpen((v) => !v)}
				className="relative flex h-10 w-10 items-center justify-center rounded-full text-on-surface transition-colors hover:bg-surface-container-low"
			>
				<Icon name="notifications" />
				{unreadCount > 0 && (
					<span
						className="absolute right-2 top-2 h-2 w-2 rounded-full bg-primary"
						aria-hidden="true"
					/>
				)}
			</button>

			{open && (
				<div className="absolute right-0 top-full z-50 mt-2 w-80 overflow-hidden rounded-xl border border-outline-variant/30 bg-surface-container-lowest shadow-card-hover">
					<div className="flex items-center justify-between border-b border-outline-variant/50 px-4 py-3">
						<h2 className="text-label-md font-label-md font-bold text-on-surface">
							알림
						</h2>
						<button
							type="button"
							onClick={markAllRead}
							className="text-caption font-caption text-primary hover:underline"
						>
							모두 읽음
						</button>
					</div>
					<div className="max-h-96 divide-y divide-outline-variant/30 overflow-y-auto">
						{preview.map((n) => (
							<button
								key={n.id}
								type="button"
								onClick={() => handleNotificationClick(n)}
								className={cn(
									"flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-surface-container-low",
									!n.isRead && "bg-primary/5",
								)}
							>
								<span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-surface-container text-secondary">
									<Icon
										name={NOTIFICATION_TYPE_ICON[n.type]}
										className="text-[18px]"
									/>
								</span>
								<div className="min-w-0 flex-1">
									<p
										className={cn(
											"text-caption font-caption text-on-surface",
											!n.isRead && "font-bold",
										)}
									>
										{n.message}
									</p>
									<p
										className={cn(
											"mt-0.5 text-[11px]",
											!n.isRead ? "text-primary" : "text-secondary",
										)}
									>
										{formatRelativeTime(n.createdAt)}
									</p>
								</div>
							</button>
						))}
						{preview.length === 0 && (
							<p className="px-4 py-8 text-center text-caption font-caption text-secondary">
								알림이 없어요.
							</p>
						)}
					</div>
					<Link
						to={paths.notifications}
						onClick={() => setOpen(false)}
						className="block border-t border-outline-variant/50 px-4 py-3 text-center text-label-md font-label-md text-secondary hover:bg-surface-container-low hover:text-primary"
					>
						이전 알림 더보기
					</Link>
				</div>
			)}
		</div>
	);
}
