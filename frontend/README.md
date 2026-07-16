# 한땀한땀 Frontend

React + Vite + TypeScript + Tailwind CSS 기반 프론트엔드.
stitch 시안(`handdam_front/` 19개 HTML)을 디자인 시스템 + 컴포넌트로 이관하는 프로젝트.

## 실행

패키지 매니저는 **bun** 사용 (`bun.lock` 커밋). npm 아님.

```bash
cd frontend
bun install
bun run dev      # http://localhost:5173  (/api 는 :8080 백엔드로 프록시)
bun run build    # 타입체크 + 프로덕션 빌드
```

`.env` 는 `.env.sample` 참고 (`VITE_API_BASE_URL`, `VITE_WS_BASE_URL`, `VITE_TOSS_CLIENT_KEY`).

## 디자인 시스템 (중요)

- **토큰 단일 출처(SSOT): [`tailwind.config.js`](./tailwind.config.js)**. 색/타이포/간격/radius/그림자를 여기서만 정의한다. 페이지에 hex 하드코딩 금지 — 토큰 클래스(`bg-primary`, `text-secondary`, `rounded-xl` …)만 사용.
- **브랜드 레드**: `primary` = `#C62839`, hover/눌림 = `primary-hover`(`#A30424`).
- **폰트**: 한글 `Pretendard` + 라틴 `Hanken Grotesk` 페어링(`font-sans`). 큰 제목은 `font-display`(Hanken 우선).
- 출처 문서: `../../stitch_creator_subscription_management_platform/artisan_stitch/DESIGN.md` (초기 토큰의 원본. 코드와 불일치 시 코드가 최신).

## 폴더 구조

```
src/
  app/         router.tsx(라우트 정의), paths.ts(경로 상수 — 링크는 반드시 여기 경유)
  layouts/     PublicLayout / ConsumerLayout / DashboardLayout  (3개 존)
  components/
    ui/        Button, Card, Chip, Avatar, Input, Icon  (재사용 프리미티브)
    nav/       Header, BottomTabBar, DashboardSidebar, Footer
  pages/       화면별 컴포넌트 (아직 미이관은 PagePlaceholder)
  lib/         api.ts(axios+JWT+ApiResponse 언래핑), types.ts, cn.ts
  styles/      index.css (Tailwind + base)
```

### 3개 레이아웃 존
| 존 | 레이아웃 | 네비게이션 | 화면 |
|----|----------|-----------|------|
| 비로그인 | PublicLayout | 미니멀 헤더 | 랜딩/로그인/회원가입 |
| 소비자 | ConsumerLayout | 상단 헤더 + 하단 탭바(모바일) | 홈/피드/크리에이터/게시물/구독/알림/채팅/마이페이지 |
| 크리에이터 | DashboardLayout | 좌측 사이드바(데스크톱) | 프로젝트/게시물 관리, 작성 |

## 남은 화면 이관 방법

`src/pages/PagePlaceholder` 로 걸려 있는 라우트를 실제 컴포넌트로 교체하면 된다. 예시는
[`LandingPage.tsx`](./src/pages/LandingPage.tsx), [`HomePage.tsx`](./src/pages/HomePage.tsx) 참고.

1. `handdam_front/<원본>.html` 을 열어 구조 확인 (PagePlaceholder 에 원본 파일명 표기됨).
2. 인라인 Tailwind 대신 `components/ui` 프리미티브로 재구성.
3. 색/폰트/간격은 토큰 클래스만 사용 (hex 금지).
4. `router.tsx` 에서 해당 `PagePlaceholder` 를 새 페이지로 교체.
5. 데이터는 우선 목업 → 이후 `lib/api.ts` 로 백엔드 연동.

## API 연동

`lib/api.ts` 의 `http`(axios) + `unwrap()` 사용. 백엔드 응답은 `{ success, data, error }` 구조이며
`unwrap()` 이 `data` 만 반환하고 실패 시 `ApiError` 를 throw 한다. JWT 는 요청 인터셉터가 자동 첨부.
> API 매핑은 프론트↔백엔드 양방향으로 점진 조정 예정 (투표/프로필/알림/유료결제 등 일부 미확정).
```
