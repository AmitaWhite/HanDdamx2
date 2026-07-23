import { createBrowserRouter } from "react-router-dom";
import { AuthOnlyRoute } from "@/features/auth/AuthOnlyRoute";
import { CreatorOnlyRoute } from "@/features/auth/CreatorOnlyRoute";
import { GuestOnlyRoute } from "@/features/auth/GuestOnlyRoute";
import { ConsumerLayout } from "@/layouts/ConsumerLayout";
import { PublicLayout } from "@/layouts/PublicLayout";
import { ChatPage } from "@/pages/ChatPage";
import { CreatePostPage } from "@/pages/CreatePostPage";
import { CreatorPage } from "@/pages/CreatorPage";
import { DashboardPostsPage } from "@/pages/DashboardPostsPage";
import { DashboardProjectPage } from "@/pages/DashboardProjectPage";
import { DashboardProjectsPage } from "@/pages/DashboardProjectsPage";
import { EditPostPage } from "@/pages/EditPostPage";
import { EmailVerifyPage } from "@/pages/EmailVerifyPage";
import { ForgotPasswordPage } from "@/pages/ForgotPasswordPage";
import { HomePage } from "@/pages/HomePage";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { MyPage } from "@/pages/MyPage";
import { MyPageSettingsPage } from "@/pages/MyPageSettingsPage";
import { MySubscriptionsPage } from "@/pages/MySubscriptionsPage";
import { MyPaymentsPage } from "@/pages/MyPaymentsPage";
import { MyQnaPage } from "@/pages/MyQnaPage";
import { NotificationsPage } from "@/pages/NotificationsPage";
import { OAuthCallbackPage } from "@/pages/OAuthCallbackPage";
import { PagePlaceholder } from "@/pages/PagePlaceholder";
import { PaymentFailPage } from "@/pages/PaymentFailPage";
import { PaymentSuccessPage } from "@/pages/PaymentSuccessPage";
import { PostDetailPage } from "@/pages/PostDetailPage";
import { QnaBoardPage } from "@/pages/QnaBoardPage";
import { QnaCreatePage } from "@/pages/QnaCreatePage";
import { QnaPostPage } from "@/pages/QnaPostPage";
import { ResetPasswordPage } from "@/pages/ResetPasswordPage";
import { SignupPage } from "@/pages/SignupPage";
import { SubscribeCompletePage } from "@/pages/SubscribeCompletePage";
import { SubscribeFeedPage } from "@/pages/SubscribeFeedPage";
import { SubscribeSelectPage } from "@/pages/SubscribeSelectPage";
import { AdminCreatorApplicationsPage } from "@/pages/AdminCreatorApplicationsPage";
import { AdminCategoryPage } from "@/pages/AdminCategoryPage";
import { AdminOnlyRoute } from "@/features/auth/AdminOnlyRoute";
import { paths } from "./paths";

/**
 * 라우트 = public / consumer 2개 존으로 정리한 결과.
 * 크리에이터 관리 화면(프로젝트·게시물 관리, 새 글 작성)도 별도 대시보드 레이아웃 없이
 * consumer 존(ConsumerLayout) 아래에서 CreatorOnlyRoute로 가드된다.
 * 단, MyPageSideNav는 목록형 관리 화면(DashboardProjectsPage/DashboardPostsPage)에만 표시되고,
 * 상세/작성 화면(DashboardProjectPage/CreatePostPage)은 참고 시안대로 사이드바 없이 전체 너비로 렌더한다.
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
			{ path: paths.resetPassword, element: <ResetPasswordPage /> },
			{ path: paths.oauthCallback, element: <OAuthCallbackPage /> },
			{ path: paths.emailVerify, element: <EmailVerifyPage /> },
		],
	},
	{
		element: <ConsumerLayout />,
		children: [
			// 게스트도 둘러볼 수 있는 화면 — 가드 없음
			{ path: paths.home, element: <HomePage /> },
			{ path: paths.creator(), element: <CreatorPage /> },
			{ path: paths.postDetail(), element: <PostDetailPage /> },
			// 유료 Q&A — 로그인 필요 (백엔드도 JWT + 유료 구독/크리에이터만 목록·상세 허용)
			{
				path: paths.creatorQna(),
				element: (
					<AuthOnlyRoute>
						<QnaBoardPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.qnaPost(),
				element: (
					<AuthOnlyRoute>
						<QnaPostPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.creatorQnaNew(),
				element: (
					<AuthOnlyRoute>
						<QnaCreatePage />
					</AuthOnlyRoute>
				),
			},
			// 로그인 필요 — 비로그인 시 /login 리다이렉트
			{
				path: paths.feed,
				element: (
					<AuthOnlyRoute>
						<SubscribeFeedPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.notifications,
				element: (
					<AuthOnlyRoute>
						<NotificationsPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.chat,
				element: (
					<AuthOnlyRoute>
						<ChatPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.mypage,
				element: (
					<AuthOnlyRoute>
						<MyPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.mypageSettings,
				element: (
					<AuthOnlyRoute>
						<MyPageSettingsPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.mySubscriptions,
				element: (
					<AuthOnlyRoute>
						<MySubscriptionsPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.myPayments,
				element: (
					<AuthOnlyRoute>
						<MyPaymentsPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.myQna,
				element: (
					<AuthOnlyRoute>
						<MyQnaPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.subscribeSelect(),
				element: (
					<AuthOnlyRoute>
						<SubscribeSelectPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.subscribeComplete,
				element: (
					<AuthOnlyRoute>
						<SubscribeCompletePage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.paymentSuccess,
				element: (
					<AuthOnlyRoute>
						<PaymentSuccessPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.paymentFail,
				element: (
					<AuthOnlyRoute>
						<PaymentFailPage />
					</AuthOnlyRoute>
				),
			},
			// 크리에이터 관리 화면 — 마이페이지 체계(ConsumerLayout) 아래로 통합
			{
				path: paths.dashboardProjects,
				element: (
					<AuthOnlyRoute>
						<CreatorOnlyRoute>
							<DashboardProjectsPage />
						</CreatorOnlyRoute>
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.dashboardProject(),
				element: (
					<AuthOnlyRoute>
            <DashboardProjectPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.dashboardPosts,
				element: (
					<AuthOnlyRoute>
						<DashboardPostsPage />
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.dashboardPostNew,
				element: (
					<AuthOnlyRoute>
						<CreatorOnlyRoute>
							<CreatePostPage />
						</CreatorOnlyRoute>
					</AuthOnlyRoute>
				),
			},
			{
				path: paths.dashboardPostEdit(),
				element: (
					<AuthOnlyRoute>
						<CreatorOnlyRoute>
							<EditPostPage />
						</CreatorOnlyRoute>
					</AuthOnlyRoute>
				),
			},
		],
	},
  {
    // 어드민 전용
    element: (
      <AdminOnlyRoute>
        <ConsumerLayout />
      </AdminOnlyRoute>
    ),
    children: [
      {
        path: paths.adminCreatorApplications,
        element: <AdminCreatorApplicationsPage />,
      },
      {
        path: paths.adminCategories,
        element: <AdminCategoryPage />,
      },
    ],
  },
	{ path: "*", element: <PagePlaceholder title="404" /> },
]);
