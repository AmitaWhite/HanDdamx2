/**
 * 한땀한땀 디자인 토큰 — 단일 출처(SSOT)
 * 출처: artisan_stitch/DESIGN.md 를 정리·조정한 값.
 *  - 브랜드 레드 확정: primary = #C62839 (밝은 레드), primary-hover = #A30424 (눌린 상태)
 *  - 한글 폰트 Pretendard 를 모든 스택 앞/뒤에 배치 (Hanken 은 라틴 전용)
 * 색을 바꾸려면 오직 이 파일만 수정하면 전 화면에 반영됩니다.
 */
/** @type {import('tailwindcss').Config} */
export default {
	darkMode: "class",
	content: ["./index.html", "./src/**/*.{ts,tsx}"],
	theme: {
		extend: {
			colors: {
				// --- Brand ---
				primary: {
					DEFAULT: "#c62839", // 밝은 브랜드 레드 (로고/CTA/활성 상태)
					hover: "#a30424", // 눌림/hover 시 어두운 레드
					container: "#ffdad9",
					fixed: "#ffdad9",
					"fixed-dim": "#ffb3b2",
				},
				"on-primary": {
					DEFAULT: "#ffffff",
					container: "#93000a",
					fixed: "#410008",
					"fixed-variant": "#92001e",
				},
				"inverse-primary": "#ffb3b2",
				"surface-tint": "#c62839",

				// --- Secondary / Tertiary ---
				secondary: {
					DEFAULT: "#5f5e5f",
					container: "#e2dfe0",
					fixed: "#e5e2e3",
					"fixed-dim": "#c8c6c7",
				},
				"on-secondary": {
					DEFAULT: "#ffffff",
					container: "#636263",
					fixed: "#1b1b1c",
					"fixed-variant": "#474647",
				},
				tertiary: {
					DEFAULT: "#4e5051",
					container: "#666869",
					fixed: "#e1e3e4",
					"fixed-dim": "#c5c7c8",
				},
				"on-tertiary": {
					DEFAULT: "#ffffff",
					container: "#e6e8e9",
					fixed: "#191c1d",
					"fixed-variant": "#454748",
				},

				// --- Surface / Background ---
				background: "#fbf9f8",
				"on-background": "#1b1c1c",
				surface: {
					DEFAULT: "#fbf9f8",
					dim: "#dbdad9",
					bright: "#fbf9f8",
					variant: "#e4e2e2",
					"container-lowest": "#ffffff",
					"container-low": "#f5f3f3",
					container: "#efeded",
					"container-high": "#e9e8e7",
					"container-highest": "#e4e2e2",
				},
				"on-surface": {
					DEFAULT: "#1b1c1c",
					variant: "#5a4040",
				},
				"inverse-surface": "#303031",
				"inverse-on-surface": "#f2f0f0",

				// --- Outline ---
				outline: {
					DEFAULT: "#8e706f",
					variant: "#e3bebd",
				},

				// --- Error ---
				error: {
					DEFAULT: "#ba1a1a",
					container: "#ffdad6",
				},
				"on-error": {
					DEFAULT: "#ffffff",
					container: "#93000a",
				},
			},
			fontFamily: {
				// Pretendard(한글) + Hanken(라틴) 페어링 — DESIGN.md 지침 반영
				sans: ["Pretendard", "Hanken Grotesk", "system-ui", "sans-serif"],
				display: ["Hanken Grotesk", "Pretendard", "system-ui", "sans-serif"],
			},
			fontSize: {
				"display-lg": [
					"48px",
					{ lineHeight: "1.2", letterSpacing: "-0.02em", fontWeight: "800" },
				],
				"headline-lg": ["32px", { lineHeight: "1.3", fontWeight: "700" }],
				"headline-lg-mobile": [
					"26px",
					{ lineHeight: "1.3", fontWeight: "700" },
				],
				"headline-md": ["24px", { lineHeight: "1.4", fontWeight: "600" }],
				"body-lg": ["18px", { lineHeight: "1.6", fontWeight: "400" }],
				"body-md": ["16px", { lineHeight: "1.6", fontWeight: "400" }],
				"label-md": [
					"14px",
					{ lineHeight: "1.2", letterSpacing: "0.01em", fontWeight: "600" },
				],
				caption: ["12px", { lineHeight: "1.4", fontWeight: "400" }],
			},
			borderRadius: {
				sm: "0.25rem",
				DEFAULT: "0.5rem", // 버튼/입력/작은 썸네일
				lg: "0.75rem",
				xl: "1rem", // 카드/프로필 헤더
				"2xl": "1.5rem",
				full: "9999px",
			},
			spacing: {
				base: "8px",
				gutter: "24px",
				"margin-mobile": "20px",
				"margin-desktop": "40px",
				"section-gap": "80px",
			},
			maxWidth: {
				container: "1200px",
			},
			boxShadow: {
				// DESIGN.md Elevation: 부드러운 앰비언트 그림자
				card: "0px 4px 20px rgba(0, 0, 0, 0.04)",
				"card-hover": "0px 8px 30px rgba(0, 0, 0, 0.08)",
			},
		},
	},
	plugins: [],
};
