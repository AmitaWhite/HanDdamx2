import { createBrowserRouter } from "react-router-dom";
import { ConsumerLayout } from "@/layouts/ConsumerLayout";
import { DashboardLayout } from "@/layouts/DashboardLayout";
import { PublicLayout } from "@/layouts/PublicLayout";
import { HomePage } from "@/pages/HomePage";
import { LandingPage } from "@/pages/LandingPage";
import { LoginPage } from "@/pages/LoginPage";
import { OAuthCallbackPage } from "@/pages/OAuthCallbackPage";
import { PagePlaceholder } from "@/pages/PagePlaceholder";
import { paths } from "./paths";

/**
 * 라우트 = stitch 19개 화면을 3개 존으로 정리한 결과.
 * 아직 이관 안 된 화면은 PagePlaceholder(원본 파일명 표기)로 둔다.
 */
export const router = createBrowserRouter([
	{
		element: <PublicLayout />,
		children: [
			{ path: paths.landing, element: <LandingPage /> },
			{ path: paths.login, element: <LoginPage /> },
			{
				path: paths.signup,
				element: <PagePlaceholder title="회원가입" source="signup.html" />,
			},
			{
				path: paths.forgotPassword,
				element: <PagePlaceholder title="비밀번호 찾기" />,
			},
			{ path: paths.oauthCallback, element: <OAuthCallbackPage /> },
		],
	},
	{
		element: <ConsumerLayout />,
		children: [
			{ path: paths.home, element: <HomePage /> },
			{
				path: paths.feed,
				element: (
					<PagePlaceholder title="구독 피드" source="subscribe-feed.html" />
				),
			},
			{
				path: paths.notifications,
				element: <PagePlaceholder title="알림" source="notifications.html" />,
			},
			{
				path: paths.chat,
				element: <PagePlaceholder title="메시지" source="chat.html" />,
			},
			{
				path: paths.mypage,
				element: <PagePlaceholder title="마이페이지" source="mypage.html" />,
			},
			{
				path: paths.mypageSettings,
				element: (
					<PagePlaceholder title="프로필 설정" source="mypage-settings.html" />
				),
			},
			{
				path: paths.creator(),
				element: (
					<PagePlaceholder title="크리에이터 상세" source="creator.html" />
				),
			},
			{
				path: paths.creatorQna(),
				element: <PagePlaceholder title="Q&A 게시판" source="qna-board.html" />,
			},
			{
				path: paths.qnaPost(),
				element: <PagePlaceholder title="Q&A 게시글" source="qna-post.html" />,
			},
			{
				path: paths.postDetail(),
				element: (
					<PagePlaceholder title="게시물 상세" source="post-detail.html" />
				),
			},
			{
				path: paths.subscribeSelect(),
				element: (
					<PagePlaceholder title="구독 선택" source="subscribe-select.html" />
				),
			},
			{
				path: paths.subscribeComplete,
				element: (
					<PagePlaceholder title="구독 완료" source="subscribe-complete.html" />
				),
			},
		],
	},
	{
		element: <DashboardLayout />,
		children: [
			{
				path: paths.dashboardProjects,
				element: (
					<PagePlaceholder title="프로젝트 목록" source="project-list.html" />
				),
			},
			{
				path: paths.dashboardProject(),
				element: (
					<PagePlaceholder title="프로젝트 상세" source="project-detail.html" />
				),
			},
			{
				path: paths.dashboardPosts,
				element: (
					<PagePlaceholder title="게시물 관리" source="post-management.html" />
				),
			},
			{
				path: paths.dashboardPostNew,
				element: (
					<PagePlaceholder title="새 게시물 작성" source="create-post.html" />
				),
			},
		],
	},
	{ path: "*", element: <PagePlaceholder title="404" /> },
]);
