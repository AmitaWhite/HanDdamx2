import path from "node:path";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
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
		// 백엔드(:8080)로의 프록시 — 개발 중 CORS 없이 /api 호출
		proxy: {
			"/api": {
				target: "http://localhost:8080",
				changeOrigin: true,
			},
			// SockJS/STOMP (채팅·알림) — backendOrigin이 빈 문자열일 때 /ws 로 접속
			"/ws": {
				target: "http://localhost:8080",
				changeOrigin: true,
				ws: true,
			},
		},
	},
});
