import type { ReactNode } from "react";
import {
	createContext,
	useCallback,
	useContext,
	useEffect,
	useRef,
	useState,
} from "react";
import { useAuth } from "@/features/auth/AuthContext";
import { connectNotificationSocket } from "./notificationSocket";
import {
	getMyNotifications,
	getUnreadCount,
	markAllNotificationsRead,
	markNotificationRead,
	type NotificationResponse,
} from "./notificationsApi";

interface NotificationsContextValue {
	notifications: NotificationResponse[];
	unreadCount: number;
	hasNext: boolean;
	loading: boolean;
	loadMore: () => Promise<void>;
	markOneRead: (id: number) => Promise<void>;
	markAllRead: () => Promise<void>;
}

const NotificationsContext = createContext<NotificationsContextValue | null>(
	null,
);

export function NotificationsProvider({ children }: { children: ReactNode }) {
	const { isAuthenticated, accessToken, user } = useAuth();
	const [notifications, setNotifications] = useState<NotificationResponse[]>(
		[],
	);
	const [unreadCount, setUnreadCount] = useState(0);
	const [page, setPage] = useState(0);
	const [hasNext, setHasNext] = useState(false);
	const [loading, setLoading] = useState(false);
	const disconnectRef = useRef<(() => void) | null>(null);

	useEffect(() => {
		if (!isAuthenticated || !accessToken || !user) {
			setNotifications([]);
			setUnreadCount(0);
			setPage(0);
			setHasNext(false);
			disconnectRef.current?.();
			disconnectRef.current = null;
			return;
		}

		let cancelled = false;
		setLoading(true);
		Promise.all([getUnreadCount(), getMyNotifications({ page: 0 })])
			.then(([unread, slice]) => {
				if (cancelled) return;
				setUnreadCount(unread.unreadCount);
				setNotifications(slice.content);
				setHasNext(slice.hasNext);
				setPage(0);
			})
			.finally(() => {
				if (!cancelled) setLoading(false);
			});

		disconnectRef.current = connectNotificationSocket(
			user.memberId,
			accessToken,
			(notification) => {
				setNotifications((prev) => [notification, ...prev]);
				setUnreadCount((prev) => prev + 1);
			},
		);

		return () => {
			cancelled = true;
			disconnectRef.current?.();
			disconnectRef.current = null;
		};
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [isAuthenticated, accessToken, user?.memberId]);

	const loadMore = useCallback(async () => {
		if (!hasNext || loading) return;
		setLoading(true);
		try {
			const nextPage = page + 1;
			const slice = await getMyNotifications({ page: nextPage });
			setNotifications((prev) => [...prev, ...slice.content]);
			setHasNext(slice.hasNext);
			setPage(nextPage);
		} finally {
			setLoading(false);
		}
	}, [hasNext, loading, page]);

	const markOneRead = useCallback(async (id: number) => {
		await markNotificationRead(id);
		let wasUnread = false;
		setNotifications((prev) =>
			prev.map((n) => {
				if (n.id !== id) return n;
				wasUnread = !n.isRead;
				return { ...n, isRead: true };
			}),
		);
		if (wasUnread) setUnreadCount((prev) => Math.max(0, prev - 1));
	}, []);

	const markAllRead = useCallback(async () => {
		await markAllNotificationsRead();
		setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
		setUnreadCount(0);
	}, []);

	return (
		<NotificationsContext.Provider
			value={{
				notifications,
				unreadCount,
				hasNext,
				loading,
				loadMore,
				markOneRead,
				markAllRead,
			}}
		>
			{children}
		</NotificationsContext.Provider>
	);
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() {
	const ctx = useContext(NotificationsContext);
	if (!ctx)
		throw new Error(
			"useNotifications 는 NotificationsProvider 안에서만 사용할 수 있습니다.",
		);
	return ctx;
}
