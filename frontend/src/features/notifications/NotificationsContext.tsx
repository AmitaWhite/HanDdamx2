import type { ReactNode } from "react";
import { createContext, useContext, useMemo, useState } from "react";
import { mockNotifications, type MockNotification } from "@/mocks/notifications";

interface NotificationsContextValue {
	notifications: MockNotification[];
	unreadCount: number;
	markAllRead: () => void;
}

const NotificationsContext = createContext<NotificationsContextValue | null>(null);

/** 헤더 드롭다운과 전체 알림 페이지가 같은 읽음 상태를 공유하도록 세션 내에서 끌어올린 상태. */
export function NotificationsProvider({ children }: { children: ReactNode }) {
	const [notifications, setNotifications] = useState<MockNotification[]>(mockNotifications);

	function markAllRead() {
		setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
	}

	const unreadCount = useMemo(() => notifications.filter((n) => !n.isRead).length, [notifications]);

	return (
		<NotificationsContext.Provider value={{ notifications, unreadCount, markAllRead }}>
			{children}
		</NotificationsContext.Provider>
	);
}

// eslint-disable-next-line react-refresh/only-export-components
export function useNotifications() {
	const ctx = useContext(NotificationsContext);
	if (!ctx) throw new Error("useNotifications 는 NotificationsProvider 안에서만 사용할 수 있습니다.");
	return ctx;
}
