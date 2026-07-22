import path from "node:path";
import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
	const env = loadEnv(mode, process.cwd(), "");
	// VITE_DEV_PROXY_TARGET=https://handdam.amitawhite.com 이면 로컬 dev가 배포 백엔드로 프록시
	const proxyTarget = env.VITE_DEV_PROXY_TARGET || "http://localhost:8080";

	return {
		plugins: [react()],
		// sockjs-client(STOMP 알림 소켓)가 Node의 global을 참조 — Vite는 기본적으로 폴리필하지 않아 직접 지정.
		define: {
			global: "globalThis",
		},
		resolve: {
			alias: {
				"@": path.resolve(__dirname, "./src"),
			},
		},
		server: {
			port: 5173,
			// 백엔드로의 프록시 — 개발 중 CORS 없이 /api·/ws 호출
			proxy: {
				"/api": {
					target: proxyTarget,
					changeOrigin: true,
					secure: proxyTarget.startsWith("https"),
				},
				// OAuth 리다이렉트 (구글 로그인)
				"/oauth2": {
					target: proxyTarget,
					changeOrigin: true,
					secure: proxyTarget.startsWith("https"),
				},
				"/login/oauth2": {
					target: proxyTarget,
					changeOrigin: true,
					secure: proxyTarget.startsWith("https"),
				},
				// SockJS/STOMP (채팅·알림) — wsBaseUrl 이 /ws 일 때 프록시 대상으로 전달
				"/ws": {
					target: proxyTarget,
					changeOrigin: true,
					secure: proxyTarget.startsWith("https"),
					ws: true,
				},
			},
		},
	};
});
