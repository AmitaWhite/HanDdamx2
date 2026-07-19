import { createBrowserRouter } from "react-router-dom";
import { GuestOnlyRoute } from "@/features/auth/GuestOnlyRoute";
import { ConsumerLayout } from "@/layouts/ConsumerLayout";
import { DashboardLayout } from "@/layouts/DashboardLayout";
import { PublicLayout } from "@/layouts/PublicLayout";
import { ChatPage } from "@/pages/ChatPage";
import { CreatePostPage } from "@/pages/CreatePostPage";
import { CreatorPage } from "@/pages/CreatorPage";
import { DashboardPostsPage } from "@/pages/DashboardPostsPage";
import { DashboardProjectPage } from "@/pages/DashboardProjectPage";
import { DashboardProjectsPage } from "@/pages/DashboardProjectsPage";
import { EmailVerifyPage } from "@/pages/EmailVerifyPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { HomePage } from "@/pages/HomePage";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { MyPage } from "@/pages/MyPage";
import { MyPageSettingsPage } from "@/pages/MyPageSettingsPage";
import { NotificationsPage } from "@/pages/NotificationsPage";
import { OAuthCallbackPage } from "@/pages/OAuthCallbackPage";
import { PagePlaceholder } from "@/pages/PagePlaceholder";
import { PostDetailPage } from "@/pages/PostDetailPage";
import { QnaBoardPage } from "@/pages/QnaBoardPage";
import { QnaPostPage } from "@/pages/QnaPostPage";
import { SignupPage } from "@/pages/SignupPage";
import { SubscribeCompletePage } from "@/pages/SubscribeCompletePage";
import { SubscribeFeedPage } from "@/pages/SubscribeFeedPage";
import { SubscribeSelectPage } from "@/pages/SubscribeSelectPage";
import { paths } from "./paths";

/**
 * 라우트 = stitch 19개 화면을 3개 존으로 정리한 결과.
 * 전 화면 목데이터(src/mocks/)로 채워져 있음 — 실제 API 연동은 화면별로 점진 적용 예정.
 */
export const router = createBrowserRouter([
	{
		element: <PublicLayout />,
		children: [
			{ path: paths.landing, element: <LandingPage /> },
			{
				path: paths.login,
				element: (
					<GuestOnlyRoute>
						<LoginPage />
					</GuestOnlyRoute>
				),
			},
			{
				path: paths.signup,
				element: (
					<GuestOnlyRoute>
						<SignupPage />
					</GuestOnlyRoute>
				),
			},
			{ path: paths.forgotPassword, element: <ForgotPasswordPage /> },
			{ path: paths.oauthCallback, element: <OAuthCallbackPage /> },
			{ path: paths.emailVerify, element: <EmailVerifyPage /> },
		],
	},
	{
		element: <ConsumerLayout />,
		children: [
			{ path: paths.home, element: <HomePage /> },
			{ path: paths.feed, element: <SubscribeFeedPage /> },
			{ path: paths.notifications, element: <NotificationsPage /> },
			{ path: paths.chat, element: <ChatPage /> },
			{ path: paths.mypage, element: <MyPage /> },
			{ path: paths.mypageSettings, element: <MyPageSettingsPage /> },
			{ path: paths.creator(), element: <CreatorPage /> },
			{ path: paths.creatorQna(), element: <QnaBoardPage /> },
			{ path: paths.qnaPost(), element: <QnaPostPage /> },
			{ path: paths.postDetail(), element: <PostDetailPage /> },
			{ path: paths.subscribeSelect(), element: <SubscribeSelectPage /> },
			{ path: paths.subscribeComplete, element: <SubscribeCompletePage /> },
		],
	},
	{
		element: <DashboardLayout />,
		children: [
			{ path: paths.dashboardProjects, element: <DashboardProjectsPage /> },
			{ path: paths.dashboardProject(), element: <DashboardProjectPage /> },
			{ path: paths.dashboardPosts, element: <DashboardPostsPage /> },
			{ path: paths.dashboardPostNew, element: <CreatePostPage /> },
		],
	},
	{ path: "*", element: <PagePlaceholder title="404" /> },
]);
