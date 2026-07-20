import React from "react";
import ReactDOM from "react-dom/client";
import { RouterProvider } from "react-router-dom";
import { router } from "./app/router";
import { AuthProvider } from "./features/auth/AuthContext";
import { NotificationsProvider } from "./features/notifications/NotificationsContext";
import { SubscriptionProvider } from "./features/subscription/SubscriptionContext";
import "./styles/index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
	<React.StrictMode>
		<AuthProvider>
			<NotificationsProvider>
				<SubscriptionProvider>
					<RouterProvider router={router} />
				</SubscriptionProvider>
			</NotificationsProvider>
		</AuthProvider>
	</React.StrictMode>,
);
