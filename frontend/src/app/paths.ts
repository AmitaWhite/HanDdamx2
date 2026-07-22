/**
 * 라우트 경로 단일 출처. 링크는 반드시 이 상수를 통해 참조한다.
 * (stitch 19개 화면 → 3개 존으로 정리: public / consumer / dashboard)
 */
export const paths = {
	// --- Public (비로그인) ---
	landing: "/",
	login: "/login",
	signup: "/signup",
	forgotPassword: "/forgot-password",
	resetPassword: "/reset-password",
	oauthCallback: "/oauth/callback",
	emailVerify: "/email-verify",

	// --- Consumer (소비자 앱) ---
	home: "/home",
	feed: "/feed", // 구독 피드
	notifications: "/notifications",
	chat: "/chat",
	chatRoom: (roomId: number | string) => `/chat?roomId=${roomId}`,
	chatWithCreator: (creatorId: number | string) => `/chat?creatorId=${creatorId}`,
	mypage: "/mypage",
	mypageSettings: "/mypage/settings",
	mySubscriptions: "/mypage/subscriptions",
	myPayments: "/mypage/payments",
	myQna: "/mypage/qna",

	creator: (id: string | number = ":creatorId") => `/creators/${id}`,
	creatorQna: (id: string | number = ":creatorId") => `/creators/${id}/qna`,
	creatorQnaNew: (id: string | number = ":creatorId") =>
		`/creators/${id}/qna/new`,
	qnaPost: (postId: string | number = ":postId") => `/qna/${postId}`,
	postDetail: (id: string | number = ":postId") => `/posts/${id}`,
	subscribeSelect: (id: string | number = ":creatorId") =>
		`/creators/${id}/subscribe`,
	subscribeComplete: "/subscribe/complete",
	paymentSuccess: "/payments/success",
	paymentFail: "/payments/fail",

	// --- Dashboard (크리에이터) ---
	dashboardProjects: "/dashboard/projects",
	dashboardProject: (id: string | number = ":projectId") =>
		`/dashboard/projects/${id}`,
	dashboardPosts: "/dashboard/posts",
	dashboardPostNew: "/dashboard/posts/new",
	dashboardPostEdit: (id: string | number = ":feedId") =>
		`/dashboard/posts/${id}/edit`,
  adminCreatorApplications: "/admin/creator-applications",
  adminCategories: "/admin/categories",
} as const;
