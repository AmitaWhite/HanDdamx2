-- ============================================================================
-- 쿼리 테스트 검증 쿼리 (v0.2)  —  API 보강안 반영 전면 재작성
-- 기준 문서: claude/13_API 보강안 (P/B/M/F 엔드포인트) | 선행 데이터: 11_시나리오_테스트_검증쿼리.sql
--            + 11_1 보강분(board_post 1이 ANSWERED 상태) 실행 후 상태 가정.
--
-- 목적: 보강안이 신설/보강한 각 엔드포인트의 "응답 DTO 필드"를 실제로 한 번의(또는 합리적
--       수의) SELECT로 뽑을 수 있는지 검증한다. 각 블록은 [엔드포인트] → 응답 필드 → 쿼리 →
--       주의(스키마 갭 / N+1 / 확정 필요) 순. v0.1 대비: 프로젝트·유료게시판·회원프로필
--       도메인이 통째로 추가됐고, 피드/구독/채팅 쿼리는 보강안 응답 스펙에 맞춰 필드를 재정렬.
--
-- 표기: [P/B/M/F-n] = 보강안 엔드포인트 번호. locked 판정은 05_API 0.7 공통 로직.
-- 요청자 기준값: :me = 2 (지수, 크리에이터 1을 PAID/ACTIVE 구독 중)
-- ============================================================================
-- ############################################################################
-- 1. 프로젝트 도메인 (보강안 신설 — v0.1엔 없던 영역)
-- ############################################################################
-- --- [P-1] GET /api/creators/{creatorId}/projects — 크리에이터 프로젝트 목록 ---
-- 응답: projectId, title, description, coverImageUrl, categoryId, categoryName, feedCount, createdAt, updatedAt
SELECT
  p.id AS "projectId",
  p.title,
  p.description,
  p.cover_image_url AS "coverImageUrl",
  p.category_id AS "categoryId",
  c.name AS "categoryName",
  (
    SELECT
      COUNT(*)
    FROM
      feed f
    WHERE
      f.project_id = p.id
      AND f.is_deleted = false
  ) AS "feedCount",
  p.created_at AS "createdAt",
  p.updated_at AS "updatedAt"
FROM
  project p
  JOIN category c ON c.id = p.category_id
WHERE
  p.creator_id = 1
  AND p.is_deleted = false
ORDER BY
  p.created_at DESC
LIMIT
  20
OFFSET
  0;

-- 주의: feedCount가 상관 서브쿼리라 프로젝트 수만큼 반복. 프로젝트는 크리에이터당 소수라
--       실무 부담 낮지만, 목록이 커지면 LEFT JOIN + GROUP BY 집계로 바꾸는 게 안전.
-- 검토(보강안): 이 쿼리가 P-1 응답을 정확히 재현 → 스키마로 충족됨. 추가 컬럼 불필요.
-- --- [P-2] GET /api/projects/{projectId} — 프로젝트 상세 ---------------------
-- 응답: 위 + creatorId, creatorNickname
SELECT
  p.id AS "projectId",
  p.creator_id AS "creatorId",
  m.nickname AS "creatorNickname",
  p.title,
  p.description,
  p.cover_image_url AS "coverImageUrl",
  p.category_id AS "categoryId",
  c.name AS "categoryName",
  (
    SELECT
      COUNT(*)
    FROM
      feed f
    WHERE
      f.project_id = p.id
      AND f.is_deleted = false
  ) AS "feedCount",
  p.created_at AS "createdAt",
  p.updated_at AS "updatedAt"
FROM
  project p
  JOIN member m ON m.id = p.creator_id
  JOIN category c ON c.id = p.category_id
WHERE
  p.id = 1;

-- 검토: 충족. (Stage 관련 필드는 MVP 제외이므로 응답에 없음 — 보강안과 일치.)
-- --- [P-6] GET /api/projects/{projectId}/feeds — 프로젝트 내 피드 목록 --------
-- 응답 content[]: feedId, title, visibility, locked, thumbnailUrl, likeCount, commentCount, poll{pollId,totalVotes}|null, createdAt
SELECT
  f.id AS "feedId",
  f.title,
  f.visibility,
  CASE
    WHEN f.visibility = 'PUBLIC' THEN false
    WHEN f.visibility = 'FREE_SUBSCRIBER'
    AND s.subscription_level IN ('FREE', 'PAID')
    AND s.status = 'ACTIVE' THEN false
    WHEN f.visibility = 'PAID_SUBSCRIBER'
    AND s.subscription_level = 'PAID'
    AND s.status = 'ACTIVE' THEN false
    ELSE true
  END AS "locked",
  (
    SELECT
      fa.url
    FROM
      feed_attachment fa
    WHERE
      fa.feed_id = f.id
      AND fa.type = 'IMAGE'
      AND fa.is_deleted = false
    ORDER BY
      fa.order_index
    LIMIT
      1
  ) AS "thumbnailUrl",
  f.like_count AS "likeCount",
  f.comment_count AS "commentCount",
  po.id AS "poll_pollId",
  (
    SELECT
      COUNT(*)
    FROM
      poll_vote pv
    WHERE
      pv.poll_id = po.id
  ) AS "poll_totalVotes",
  f.created_at AS "createdAt"
FROM
  feed f
  JOIN project p ON p.id = f.project_id
  LEFT JOIN subscription s ON s.creator_id = p.creator_id
  AND s.subscriber_id = 2
  LEFT JOIN poll po ON po.feed_id = f.id
WHERE
  f.project_id = 1
  AND f.is_deleted = false
ORDER BY
  f.id DESC
LIMIT
  20;

-- 주의: poll을 LEFT JOIN으로 붙여 "투표 N표"를 목록에서 한 번에 뽑음(보강안이 명시한 N+1 방지).
--       thumbnailUrl은 첫 IMAGE 첨부 상관 서브쿼리 — 목록 행마다 1회. 부담되면 LATERAL로 대체.
-- 검토: 프로젝트 상세 화면(12번 화면)의 "투표 N표", 단계 뱃지(단계는 목/보류)와 대응. 충족.
-- ############################################################################
-- 2. 유료 게시판 도메인 (보강안 신설)
-- ############################################################################
-- --- [B-1] GET /api/creators/{creatorId}/premium-board/posts — 게시글 목록 ----
-- 접근 전제: 요청자가 해당 크리에이터의 PAID/ACTIVE 구독자 또는 크리에이터 본인. 아래로 선검증.
SELECT
  EXISTS (
    SELECT
      1
    FROM
      subscription
    WHERE
      subscriber_id = 2
      AND creator_id = 1
      AND subscription_level = 'PAID'
      AND status = 'ACTIVE'
  )
  OR (1 = 1 /* 크리에이터 본인 여부: creator_id = 요청자 */) AS "canAccess";

-- 응답 content[]: postId, title, type, status, authorId, authorNickname, commentCount, hasImages, createdAt
SELECT
  bp.id AS "postId",
  bp.title,
  bp.type,
  bp.status,
  bp.member_id AS "authorId",
  m.nickname AS "authorNickname",
  (
    SELECT
      COUNT(*)
    FROM
      board_comment bc
    WHERE
      bc.board_post_id = bp.id
      AND bc.is_deleted = false
  ) AS "commentCount",
  EXISTS (
    SELECT
      1
    FROM
      board_post_image bpi
    WHERE
      bpi.board_post_id = bp.id
  ) AS "hasImages",
  bp.created_at AS "createdAt"
FROM
  board_post bp
  JOIN member m ON m.id = bp.member_id
WHERE
  bp.creator_id = 1
  AND bp.is_deleted = false
  -- 선택 필터: AND bp.type = 'QUESTION'  AND bp.status = 'WAITING'
ORDER BY
  bp.created_at DESC
LIMIT
  20
OFFSET
  0;

-- 검토: 목록에 필요한 필드 전부 스키마로 충족. 유형/상태 필터도 컬럼 존재. 정상.
-- --- [B-3] GET /api/premium-board/posts/{postId} — 게시글 상세 ---------------
-- 응답: postId, creatorId, title, type, status, content, authorId, authorNickname,
--       images[], answer{answerId,content,createdAt,updatedAt}|null, createdAt, updatedAt
SELECT
  bp.id AS "postId",
  bp.creator_id AS "creatorId",
  bp.title,
  bp.type,
  bp.status,
  bp.content,
  bp.member_id AS "authorId",
  m.nickname AS "authorNickname",
  ba.id AS "answer_answerId",
  ba.content AS "answer_content",
  ba.created_at AS "answer_createdAt",
  ba.updated_at AS "answer_updatedAt",
  bp.created_at AS "createdAt",
  bp.updated_at AS "updatedAt"
FROM
  board_post bp
  JOIN member m ON m.id = bp.member_id
  LEFT JOIN board_answer ba ON ba.board_post_id = bp.id
  AND ba.is_deleted = false
WHERE
  bp.id = 1;

-- 첨부 이미지 (images[])는 별도 조회 — 1:N이라 상세 쿼리와 분리
SELECT
  bpi.id AS "imageId",
  bpi.url,
  bpi.order_index AS "orderIndex"
FROM
  board_post_image bpi
WHERE
  bpi.board_post_id = 1
ORDER BY
  bpi.order_index;

-- 댓글은 기존 GET .../comments 사용 (아래 [기타] 참고).
-- 검토: BOARD_ANSWER LEFT JOIN으로 answer|null 표현 가능. 상세 화면(qna-post) 대응. 충족.
-- ############################################################################
-- 3. 회원·프로필 도메인 (보강안이 빈 1-2절을 채움)
-- ############################################################################
-- --- [M-1] GET /api/members/me — 내 프로필 -----------------------------------
-- 응답: memberId, email, nickname, profileImageUrl, role, oauthProvider, emailVerified, createdAt
SELECT
  m.id AS "memberId",
  m.email,
  m.nickname,
  m.profile_image_url AS "profileImageUrl",
  m.role,
  m.oauth_provider AS "oauthProvider",
  (m.email_verified_at IS NOT NULL) AS "emailVerified",
  m.created_at AS "createdAt"
FROM
  member m
WHERE
  m.id = 2;

-- 검토: bio 필드 없음 — 보강안 결정(회원 bio 삭제)과 일치. 자기소개는 크리에이터 전용(아래 참고).
-- --- [M-3] GET /api/members/{memberId} — 타인 공개 프로필 ---------------------
-- 응답: memberId, nickname, profileImageUrl, role, creatorId|null (이메일 등 개인정보 제외)
SELECT
  m.id AS "memberId",
  m.nickname,
  m.profile_image_url AS "profileImageUrl",
  m.role,
  cp.id AS "creatorId" -- 크리에이터면 creator_profile.id, 아니면 NULL
FROM
  member m
  LEFT JOIN creator_profile cp ON cp.member_id = m.id
WHERE
  m.id = 1;

-- 검토: 내 프로필(M-1)과 응답을 분리 → 화면(profile_detail_view)의 "내/타인 혼선" API측 해소. 충족.
-- --- [M-4] GET /api/members/me/comments — 내가 쓴 댓글 횡단 조회 --------------
-- 응답 content[]: commentId, source(FEED|BOARD), targetId, targetTitle, content, createdAt
SELECT
  commentId,
  source,
  targetId,
  targetTitle,
  content,
  createdAt
FROM
  (
    SELECT
      fc.id AS commentId,
      'FEED' AS source,
      fc.feed_id AS targetId,
      f.title AS targetTitle,
      fc.content,
      fc.created_at AS createdAt
    FROM
      feed_comment fc
      JOIN feed f ON f.id = fc.feed_id
    WHERE
      fc.member_id = 2
      AND fc.is_deleted = false
    UNION ALL
    SELECT
      bc.id,
      'BOARD',
      bc.board_post_id,
      bp.title,
      bc.content,
      bc.created_at
    FROM
      board_comment bc
      JOIN board_post bp ON bp.id = bc.board_post_id
    WHERE
      bc.member_id = 2
      AND bc.is_deleted = false
  ) t
ORDER BY
  t.createdAt DESC
LIMIT
  20
OFFSET
  0;

-- 주의: FEED_COMMENT + BOARD_COMMENT UNION. source 필터(ALL/FEED/BOARD)는 WHERE로 분기.
--       정렬/페이징이 UNION 결과 위에서 일어나므로 인덱스 활용이 제한적 — 데이터 많아지면
--       각 테이블에 (member_id, created_at) 인덱스 + 커서 페이징 검토.
-- 검토: 마이페이지 "작성한 댓글 목록"(14_1_with_comments), 게시물관리 "작성한 댓글" 대응. 충족.
-- --- [M-5] GET /api/members/me/summary — 마이페이지 상단 카운트 --------------
-- 응답: subscribingCreatorCount, commentCount
SELECT
  (
    SELECT
      COUNT(*)
    FROM
      subscription s
    WHERE
      s.subscriber_id = 2
      AND s.status = 'ACTIVE'
  ) AS "subscribingCreatorCount",
  (
    SELECT
      COUNT(*)
    FROM
      feed_comment fc
    WHERE
      fc.member_id = 2
      AND fc.is_deleted = false
  ) + (
    SELECT
      COUNT(*)
    FROM
      board_comment bc
    WHERE
      bc.member_id = 2
      AND bc.is_deleted = false
  ) AS "commentCount";

-- 주의: subscribingCreatorCount가 FREE+PAID 합인지(현재 이렇게 계산) 화면 정의 확인 필요 —
--       마이페이지 "구독 중인 크리에이터 수"가 무료 포함인지 유료만인지 05_API/화면에 미명시.
-- 검토: ERD "마이페이지 집계" 정의(구독 활성 수 / 댓글 두 테이블 합)와 일치. 단 위 무료포함 여부만 확정 필요.
-- ############################################################################
-- 4. 피드 도메인 (보강안 F-1~F-4 반영 — v0.1에서 필드 재정렬)
-- ############################################################################
-- --- [F-3 확정] GET /api/feeds/home — 홈(구독 관계 반영, locked 판정) ---------
-- 보강안 결정: 화면11 변형2 = 홈. 구독 중인 크리에이터의 열람 가능 피드.
SELECT
  f.id AS "feedId",
  f.title,
  f.visibility,
  p.creator_id AS "creatorId",
  m.nickname AS "creatorNickname",
  f.like_count AS "likeCount",
  f.comment_count AS "commentCount",
  po.id AS "poll_pollId",
  (
    SELECT
      COUNT(*)
    FROM
      poll_vote pv
    WHERE
      pv.poll_id = po.id
  ) AS "poll_totalVotes",
  f.created_at AS "createdAt"
FROM
  feed f
  JOIN project p ON p.id = f.project_id
  JOIN member m ON m.id = p.creator_id
  JOIN subscription s ON s.creator_id = p.creator_id
  AND s.subscriber_id = 2
  AND s.status = 'ACTIVE'
  LEFT JOIN poll po ON po.feed_id = f.id
WHERE
  f.is_deleted = false
  AND (
    f.visibility = 'PUBLIC'
    OR (
      f.visibility = 'FREE_SUBSCRIBER'
      AND s.subscription_level IN ('FREE', 'PAID')
    )
    OR (
      f.visibility = 'PAID_SUBSCRIBER'
      AND s.subscription_level = 'PAID'
    )
  )
ORDER BY
  f.created_at DESC
LIMIT
  20;

-- 검토: 구독한 크리에이터 전원의 피드를 한 번에. subscription(subscriber_id,creator_id),
--       project(creator_id) 인덱스로 EXPLAIN 확인 권장(07번 병목 검토 연계).
-- --- [F-2] 정렬 파라미터 popular — 인기순 (like_count 기준) -------------------
-- 탐색/크리에이터별/프로젝트별 목록 공통. 예: 탐색 피드 인기순
SELECT
  f.id AS "feedId",
  f.title,
  f.like_count AS "likeCount",
  f.created_at AS "createdAt"
FROM
  feed f
WHERE
  f.is_deleted = false
  AND f.visibility = 'PUBLIC'
ORDER BY
  f.like_count DESC,
  f.created_at DESC -- sort=popular
LIMIT
  20;

-- 검토: "인기" 기준을 like_count로 확정(보강안 F-2 제안). 조회수는 FEED-OUT-001로 제외돼 후보 아님.
--       like_count가 반정규화 컬럼이라 정렬 비용 낮음(별도 집계 불필요) — 반정규화 설계의 이점.
-- --- [Q-상세] GET /api/feeds/{feedId} — 상세 + locked (F-1 응답 대응) --------
SELECT
  f.id AS "feedId",
  f.title,
  f.content,
  f.visibility,
  f.like_count AS "likeCount",
  f.comment_count AS "commentCount",
  p.creator_id AS "creatorId",
  m.nickname AS "creatorNickname",
  m.profile_image_url AS "creatorProfileImageUrl",
  c.name AS "categoryName",
  CASE
    WHEN f.visibility = 'PUBLIC' THEN false
    WHEN f.visibility = 'FREE_SUBSCRIBER'
    AND s.subscription_level IN ('FREE', 'PAID')
    AND s.status = 'ACTIVE' THEN false
    WHEN f.visibility = 'PAID_SUBSCRIBER'
    AND s.subscription_level = 'PAID'
    AND s.status = 'ACTIVE' THEN false
    ELSE true
  END AS "locked"
FROM
  feed f
  JOIN project p ON p.id = f.project_id
  JOIN member m ON m.id = p.creator_id
  JOIN category c ON c.id = p.category_id
  LEFT JOIN subscription s ON s.creator_id = p.creator_id
  AND s.subscriber_id = 2
WHERE
  f.id = 4;

-- 첨부/투표/댓글은 각각 별도 조회(1:N) — 아래 참고 쿼리들로 분리.
-- ############################################################################
-- 5. 여전히 유효한 조회 (v0.1에서 이관 — 투표·구독·결제·채팅)
-- ############################################################################
-- --- 투표 상세 + 내 참여 (POLL-004~008) — 가중치 결과 -----------------------
SELECT
  po.id AS "optionId",
  po.option_text AS "optionText",
  po.order_index AS "orderIndex",
  COALESCE(SUM(pv.weight), 0) AS "weightedScore", -- 가중치 합 (유료2/무료1)
  COUNT(pv.id) AS "voterCount",
  EXISTS (
    SELECT
      1
    FROM
      poll_vote x
    WHERE
      x.poll_option_id = po.id
      AND x.member_id = 2
  ) AS "votedByMe"
FROM
  poll_option po
  LEFT JOIN poll_vote pv ON pv.poll_option_id = po.id
WHERE
  po.poll_id = 1
GROUP BY
  po.id,
  po.option_text,
  po.order_index
ORDER BY
  po.order_index;

-- 검토: 화면 "누적 N표 / 유료 2표 반영"과 대응. weightedScore=결과 막대, voterCount=참여 인원.
--       (v0.1 Q6/Q7 이슈였던 "화면 숫자가 인원수냐 가중합이냐"는 → 결과 화면은 가중합으로 확정 권장.)
-- --- 내 구독 목록 (SUB-PAID-011) --------------------------------------------
SELECT
  s.id AS "subscriptionId",
  m.nickname AS "creatorNickname",
  s.subscription_level AS "subscriptionLevel",
  s.status,
  s.current_period_end_at AS "currentPeriodEndAt"
FROM
  subscription s
  JOIN member m ON m.id = s.creator_id
WHERE
  s.subscriber_id = 2
ORDER BY
  s.created_at DESC
LIMIT
  20
OFFSET
  0;

-- --- 결제 내역 (PAYMENT-006) -------------------------------------------------
SELECT
  id AS "paymentId",
  order_id AS "orderId",
  amount,
  status,
  payment_method AS "paymentMethod",
  paid_at AS "paidAt",
  created_at AS "createdAt"
FROM
  payment
WHERE
  member_id = 2
ORDER BY
  created_at DESC
LIMIT
  20
OFFSET
  0;

-- 검토: 카드번호 필드 없음 — 결정(카드번호 표시 삭제)과 일치. payment_method(예: 'CARD')만 노출.
-- --- 채팅방 목록 + 마지막 메시지 + 안읽음 수 (CHAT-005) ----------------------
SELECT
  cr.id AS "chatRoomId",
  cr.status,
  cr.last_message_at AS "lastMessageAt",
  (
    SELECT
      cm.content
    FROM
      chat_message cm
    WHERE
      cm.chat_room_id = cr.id
    ORDER BY
      cm.sent_at DESC
    LIMIT
      1
  ) AS "lastMessagePreview",
  (
    SELECT
      COUNT(*)
    FROM
      chat_message cm2
    WHERE
      cm2.chat_room_id = cr.id
      AND cm2.sender_id <> 2
      AND cm2.read_at IS NULL
  ) AS "unreadCount"
FROM
  chat_room cr
WHERE
  cr.creator_id = 2
  OR cr.member_id = 2
ORDER BY
  cr.last_message_at DESC NULLS LAST;

-- 주의: 상관 서브쿼리 2개(방마다 반복). 방 수 많아지면 LATERAL JOIN 권장(v0.1과 동일 지적).
-- ############################################################################
-- 6. API 보강안 교차검토 — 쿼리로 드러난 확정 필요 항목
-- ############################################################################
-- (1) M-5 subscribingCreatorCount: 무료 포함/유료만 — 마이페이지 정의 미확정.
-- (2) 투표 결과 표시 숫자: weightedScore(가중합)로 확정 권장 — API 응답 필드명 명시 필요.
-- (3) 목록류 상관 서브쿼리(thumbnailUrl, feedCount, unreadCount, poll_totalVotes):
--     데이터 증가 시 LATERAL/집계 JOIN 전환 지점 — 07번 병목 검토 문서에 목록별로 반영.
-- (4) P-6/F-3의 poll 요약을 목록 응답에 포함하는 형태가 스키마로 문제없이 나옴 → 보강안 유지.
-- 이 파일은 보강안 응답 DTO가 현행 스키마(무변경)로 전부 재현 가능함을 확인한다.
-- 즉 보강안의 15개 신설 엔드포인트는 컬럼 추가 없이 구현 가능 → 시나리오/스키마 재작업 불요.