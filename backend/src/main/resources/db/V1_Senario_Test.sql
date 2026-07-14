-- ============================================================================
-- 시나리오 테스트 검증 쿼리 (v0.1)
-- 대응 문서: V1_시나리오 테스트 - MVP(크리에이터 피드·구독)
-- 기준 스키마 : V202607122313__init_schema.sql
-- 전제: 빈 DB(fresh)에서 위에서부터 순서대로 실행. IDENTITY는 1부터 순차 증가한다고 가정.
--
-- [추가] test(H2) 프로필로 돌릴 경우 주의
-- test 프로필은 flyway.enabled=false, ddl-auto=create-drop이라 스키마가 이 SQL이 아니라
-- 그 시점까지 작성된 JPA Entity 클래스에서 파생된다. 즉:
--   - UNIQUE(subscriber_id, creator_id) 같은 복합 유니크나 CHECK(depth IN (0,1)) 같은 제약이
--     Entity에 @Table(uniqueConstraints=...)/@Check로 아직 옮겨져 있지 않으면, 위반이 예상되는
--     스텝(B-2, A-8의 depth=2 시도 등)이 에러 없이 그냥 들어가버릴 수 있다.
--   - 이건 쿼리가 틀린 게 아니라 "엔티티에 아직 그 제약이 안 옮겨졌다"는 신호로 해석해야 한다.
--   - local(Postgres, Flyway 마이그레이션 기준)로 돌리면 이 SQL이 기대한 대로 막힌다.
-- 정확한 비교를 위해선 test/local 양쪽에서 한 번씩 돌려보고 결과가 갈리는 지점을 확인하는 걸 추천.
-- ============================================================================
-- ============================================================================
-- 0. 공통 준비 데이터
-- ============================================================================
-- MEMBER.id = 1 (크리에이터, 민호)
INSERT INTO
    member (
        email,
        password,
        nickname,
        role,
        oauth_provider,
        email_verified_at,
        created_at,
        updated_at
    )
VALUES
    (
        'minho@test.com',
        'hashed_pw',
        '민호',
        'CREATOR',
        'NONE',
        now(),
        now(),
        now()
    );

-- MEMBER.id = 2 (구독자, 지수)
INSERT INTO
    member (
        email,
        password,
        nickname,
        role,
        oauth_provider,
        email_verified_at,
        created_at,
        updated_at
    )
VALUES
    (
        'jisu@test.com',
        'hashed_pw',
        '지수',
        'USER',
        'NONE',
        now(),
        now(),
        now()
    );

-- MEMBER.id = 3 (비구독자, B-9용)
INSERT INTO
    member (
        email,
        password,
        nickname,
        role,
        oauth_provider,
        email_verified_at,
        created_at,
        updated_at
    )
VALUES
    (
        'outsider@test.com',
        'hashed_pw',
        '구경꾼',
        'USER',
        'NONE',
        now(),
        now(),
        now()
    );

-- CREATOR_PROFILE.id = 1
INSERT INTO
    creator_profile (
        member_id,
        introduction,
        subscription_price,
        created_at,
        updated_at
    )
VALUES
    (1, '뜨개질 5년차입니다', 15000, now(), now());

-- CATEGORY.id = 1, 2
INSERT INTO
    category (name, is_active, created_at, updated_at)
VALUES
    ('뜨개질', true, now(), now());

INSERT INTO
    category (name, is_active, created_at, updated_at)
VALUES
    ('자수', true, now(), now());

-- PROJECT.id = 1 (뜨개질 프로젝트), PROJECT.id = 2 (자수 프로젝트, A-5 이동 대상)
INSERT INTO
    project (
        creator_id,
        category_id,
        title,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (1, 1, '겨울 목도리 만들기', false, now(), now());

INSERT INTO
    project (
        creator_id,
        category_id,
        title,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (1, 2, '자수 소품 모음', false, now(), now());

-- ============================================================================
-- A. 크리에이터 피드 생성과 관리
-- ============================================================================
-- --- A-1. 프로젝트 생성 → 피드 작성 기본 흐름 -------------------------------
-- FEED.id = 1
INSERT INTO
    feed (
        project_id,
        title,
        content,
        visibility,
        is_deleted,
        comment_count,
        like_count,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        '목도리 시작합니다',
        '오늘부터 목도리 뜨기 시작!',
        'PUBLIC',
        false,
        0,
        0,
        now(),
        now()
    );

-- 확인: FEED에 category_id가 없다 → PROJECT를 통해서만 카테고리 조회 가능한지 확인
SELECT
    f.id,
    f.title,
    p.category_id,
    c.name AS category_name
FROM
    feed f
    JOIN project p ON p.id = f.project_id
    JOIN category c ON c.id = p.category_id
WHERE
    f.id = 1;

-- --- A-2. 공개범위 3종 피드 생성 -------------------------------------------
-- FEED.id = 2, 3, 4
INSERT INTO
    feed (
        project_id,
        title,
        content,
        visibility,
        is_deleted,
        comment_count,
        like_count,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        '전체공개 피드',
        '누구나 볼 수 있음',
        'PUBLIC',
        false,
        0,
        0,
        now(),
        now()
    );

INSERT INTO
    feed (
        project_id,
        title,
        content,
        visibility,
        is_deleted,
        comment_count,
        like_count,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        '무료구독자 공개 피드',
        '무료 구독자만',
        'FREE_SUBSCRIBER',
        false,
        0,
        0,
        now(),
        now()
    );

INSERT INTO
    feed (
        project_id,
        title,
        content,
        visibility,
        is_deleted,
        comment_count,
        like_count,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        '유료구독자 공개 피드',
        '유료 구독자만 - 도안 포함',
        'PAID_SUBSCRIBER',
        false,
        0,
        0,
        now(),
        now()
    );

-- 확인용 부정 케이스: 허용되지 않은 값 삽입 시도 → DB 레벨에서 막히는지 확인
-- (CHECK 제약이 없다면 에러 없이 그냥 들어감 = 이슈)
-- 부정케이스 기동 x 확인
INSERT INTO
    feed (
        project_id,
        title,
        content,
        visibility,
        is_deleted,
        comment_count,
        like_count,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        '오타 테스트',
        '이 값은 존재하지 않는 enum',
        'PAID_SUBSCRIBERR',
        false,
        0,
        0,
        now(),
        now()
    );

-- 위 INSERT가 성공해버리면: visibility에 CHECK 제약이 없다는 뜻 → 이슈 트래커에 기록
-- (테스트 후 정리) DELETE FROM feed WHERE title = '오타 테스트';
-- --- A-3. 첨부파일 3유형 삽입 (feed_id = 4 대상) ---------------------------
INSERT INTO
    feed_attachment (
        feed_id,
        type,
        url,
        storage_key,
        order_index,
        is_deleted,
        created_at
    )
VALUES
    (
        4,
        'IMAGE',
        'https://cdn.test/img1.jpg',
        's3-key-img1',
        0,
        false,
        now()
    );

INSERT INTO
    feed_attachment (
        feed_id,
        type,
        url,
        order_index,
        is_deleted,
        created_at
    )
VALUES
    (
        4,
        'VIDEO_LINK',
        'https://youtube.com/watch?v=abc123',
        1,
        false,
        now()
    );

INSERT INTO
    feed_attachment (
        feed_id,
        type,
        url,
        storage_key,
        original_name,
        order_index,
        is_deleted,
        created_at
    )
VALUES
    (
        4,
        'FILE',
        'https://cdn.test/pattern.pdf',
        's3-key-pattern',
        '목도리_도안.pdf',
        2,
        false,
        now()
    );

-- 확인용 부정 케이스: FILE인데 storage_key를 NULL로 넣어도 막히지 않는지 확인
INSERT INTO
    feed_attachment (
        feed_id,
        type,
        url,
        storage_key,
        original_name,
        order_index,
        is_deleted,
        created_at
    )
VALUES
    (
        4,
        'FILE',
        'https://cdn.test/pattern2.pdf',
        NULL,
        '도안2.pdf',
        3,
        false,
        now()
    );

-- 성공하면: "FILE 유형은 storage_key 필수"라는 규칙이 DB엔 없고 앱에만 있다는 뜻 (A-3 확인 포인트 그대로)
-- --- A-4. 피드 수정 (본문 + 공개범위 변경, feed_id = 1) --------------------
-- [TODO] 앱레이어 이관 체크로직 검토 확인 - priority row 
UPDATE feed
SET
    content = '오늘부터 목도리 뜨기 시작! (수정: 색상 변경)',
    visibility = 'FREE_SUBSCRIBER',
    updated_at = now()
WHERE
    id = 1;

-- 확인: updated_at이 실제로 바뀌었는지 (JPA Auditing 붙이기 전엔 수동 now() 필요)
SELECT
    id,
    visibility,
    created_at,
    updated_at
FROM
    feed
WHERE
    id = 1;

-- --- A-5. 피드를 다른 프로젝트로 이동 (feed_id = 1 → project_id = 2) -------
UPDATE feed
SET
    project_id = 2,
    updated_at = now()
WHERE
    id = 1;

-- 확인: FEED에 category 컬럼이 없어서 조회 시 자동으로 새 카테고리(자수)로 바뀌어 보이는지
SELECT
    f.id,
    f.title,
    p.title AS project_title,
    c.name AS category_name
FROM
    feed f
    JOIN project p ON p.id = f.project_id
    JOIN category c ON c.id = p.category_id
WHERE
    f.id = 1;

-- "본인 소유 프로젝트로만 이동 가능"은 여기선 검증 안 됨 (project.creator_id 비교는 앱 책임)
-- --- A-6. 피드 소프트 삭제 (feed_id = 2) -----------------------------------
UPDATE feed
SET
    is_deleted = true,
    updated_at = now()
WHERE
    id = 2;

-- 확인 포인트: FEED에는 deleted_at 컬럼이 없다 → 아래 쿼리는 애초에 컬럼이 없어 실행 자체가 안 됨
-- UPDATE feed SET is_deleted = true, deleted_at = now() WHERE id = 2;  -- ERROR: column "deleted_at" not found
-- → PROJECT/FEED_ATTACHMENT와 다르게 FEED만 삭제 시각을 못 남기는 게 이슈 (10번 문서 A-6과 동일)
-- --- A-7. 활성 피드 존재 시 프로젝트 삭제 차단 (project_id = 2) ------------
-- project_id=2에는 A-5에서 옮긴 feed_id=1이 살아있음 → 삭제 시도
--
-- result - update or delete on table "project" violates foreign key constraint "fk_feed_project" on table "feed"
-- Detail: Key (id)=(2) is still referenced from table "feed".
DELETE FROM project
WHERE
    id = 2;

-- FK 제약(ON DELETE RESTRICT 등)이 없다면 이 DELETE가 그냥 성공해버림
-- 성공하면: "활성 피드 있으면 삭제 차단" 정책이 DB엔 없고 전적으로 앱 로직 의존 (이슈로 기록)
-- (테스트 후 프로젝트를 지웠다면 이후 B/A 시나리오에 영향 없도록 별도 트랜잭션에서 롤백 권장)
-- --- A-8. 피드 댓글 + 대댓글 (feed_id = 3) ---------------------------------
-- FEED_COMMENT.id = 1 (댓글, depth=0)
INSERT INTO
    feed_comment (
        feed_id,
        member_id,
        parent_comment_id,
        depth,
        content,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (
        3,
        2,
        NULL,
        0,
        '이 부분 어떻게 뜨는 건가요?',
        false,
        now(),
        now()
    );

-- FEED_COMMENT.id = 2 (대댓글, depth=1, parent=1)
INSERT INTO
    feed_comment (
        feed_id,
        member_id,
        parent_comment_id,
        depth,
        content,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (
        3,
        1,
        1,
        1,
        '겉뜨기 2코, 안뜨기 2코 반복하시면 돼요',
        false,
        now(),
        now()
    );

-- 확인용 부정 케이스 1: depth=2 (대댓글의 대댓글) 시도 → CHECK(depth IN (0,1))로 막히는지
-- result
-- new row for relation "feed_comment" violates check constraint "ck_feed_comment_depth"
-- Detail: Failing row contains (3, 3, 2, 2, 2, 오 감사합니다!, f, 2026-07-14 05:34:03.971179+00, 2026-07-14 05:34:03.971179+00, null).
INSERT INTO
    feed_comment (
        feed_id,
        member_id,
        parent_comment_id,
        depth,
        content,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (3, 2, 2, 2, '오 감사합니다!', false, now(), now());

-- 성공하면: CHECK 제약이 없거나 test(H2) 스키마엔 아직 안 옮겨졌다는 뜻
-- 확인용 부정 케이스 2: depth=1인데 parent_comment_id NULL
-- result : new row for relation "feed_comment" violates check constraint "ck_feed_comment_parent"
-- Detail: Failing row contains (4, 3, 2, null, 1, 부모 없는 대댓글, f, 2026-07-14 05:34:26.417231+00, 2026-07-14 05:34:26.417231+00, null).
INSERT INTO
    feed_comment (
        feed_id,
        member_id,
        parent_comment_id,
        depth,
        content,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (3, 2, NULL, 1, '부모 없는 대댓글', false, now(), now());

-- 성공하면: "depth 1은 parent_comment_id 필수" 제약도 DB엔 없다는 뜻
-- --- A-9. 피드 좋아요 등록/취소 + 중복 방지 (feed_id = 3) ------------------
-- FEED_LIKE.id = 1
INSERT INTO
    feed_like (feed_id, member_id, created_at)
VALUES
    (3, 2, now());

-- 확인용 부정 케이스: 동일 (feed_id, member_id) 재등록 시도
-- result : duplicate key value violates unique constraint "uq_feed_like"
-- Detail: Key (feed_id, member_id)=(3, 2) already exists.
INSERT INTO
    feed_like (feed_id, member_id, created_at)
VALUES
    (3, 2, now());

-- 기대: UNIQUE(feed_id, member_id) 위반으로 에러
-- 취소 (물리 삭제)
-- result : Success, 1 row affected => 부정동작
DELETE FROM feed_like
WHERE
    feed_id = 3
    AND member_id = 2;

-- --- A-10. 댓글·좋아요에 따른 FEED 카운트 갱신 (feed_id = 3) ---------------
-- 같은 트랜잭션이라고 가정하고 두 문장을 붙여서 실행
INSERT INTO
    feed_like (feed_id, member_id, created_at)
VALUES
    (3, 2, now());

UPDATE feed
SET
    like_count = like_count + 1
WHERE
    id = 3;

-- 확인
SELECT
    id,
    comment_count,
    like_count
FROM
    feed
WHERE
    id = 3;

-- comment_count는 A-8에서 성공한 INSERT 개수만큼 수동 반영해서 맞춰봐야 함 (실제론 앱이 갱신)
-- --- A-11. 피드 투표 생성 + 선택지 + 구독등급별 투표 (feed_id = 4) ---------
-- POLL.id = 1
INSERT INTO
    poll (
        feed_id,
        question,
        end_at,
        is_closed,
        created_at,
        updated_at
    )
VALUES
    (
        4,
        '다음 배색 어떤 게 좋을까요?',
        now() + INTERVAL '3' DAY,
        false,
        now(),
        now()
    );

-- 확인용 부정 케이스: 같은 feed_id로 POLL 2개째 시도
-- duplicate key value violates unique constraint "uq_poll_feed"
-- Detail: Key (feed_id)=(4) already exists.
INSERT INTO
    poll (
        feed_id,
        question,
        end_at,
        is_closed,
        created_at,
        updated_at
    )
VALUES
    (
        4,
        '중복 투표 시도',
        now() + INTERVAL '3' DAY,
        false,
        now(),
        now()
    );

-- 기대: UNIQUE(feed_id) 위반으로 에러
-- POLL_OPTION.id = 1~4
INSERT INTO
    poll_option (poll_id, option_text, order_index, created_at)
VALUES
    (1, '색상 A (아이보리)', 0, now());

INSERT INTO
    poll_option (poll_id, option_text, order_index, created_at)
VALUES
    (1, '색상 B (베이지)', 1, now());

INSERT INTO
    poll_option (poll_id, option_text, order_index, created_at)
VALUES
    (1, '색상 C (그레이)', 2, now());

INSERT INTO
    poll_option (poll_id, option_text, order_index, created_at)
VALUES
    (1, '색상 D (네이비)', 3, now());

-- POLL_VOTE: 무료구독자(member_id=2, 아직 B파트 전이라 구독 자체가 없음 → B-1 먼저 실행한 뒤 아래 실행 권장)
INSERT INTO
    poll_vote (
        poll_id,
        poll_option_id,
        member_id,
        weight,
        subscription_level_snapshot,
        created_at,
        updated_at
    )
VALUES
    (1, 1, 2, 1, 'FREE', now(), now());

-- ============================================================================
-- B. 구독과 이에 따른 혜택
-- (A-11의 POLL_VOTE보다 먼저 실행하고 싶다면 B-1을 A-11 앞으로 옮겨서 실행해도 무방)
-- ============================================================================
-- --- B-1. 무료 구독 신청 (subscriber=2, creator=1) -------------------------
-- SUBSCRIPTION.id = 1
INSERT INTO
    subscription (
        subscriber_id,
        creator_id,
        subscription_level,
        status,
        auto_renew,
        started_at,
        created_at,
        updated_at
    )
VALUES
    (
        2,
        1,
        'FREE',
        'ACTIVE',
        false,
        now(),
        now(),
        now()
    );

-- --- B-2. 동일 크리에이터 중복 무료 구독 차단 -------------------------------
INSERT INTO
    subscription (
        subscriber_id,
        creator_id,
        subscription_level,
        status,
        auto_renew,
        started_at,
        created_at,
        updated_at
    )
VALUES
    (
        2,
        1,
        'FREE',
        'ACTIVE',
        false,
        now(),
        now(),
        now()
    );

-- 기대: UNIQUE(subscriber_id, creator_id) 위반으로 에러
-- (test/H2에서 성공해버리면 → 아직 이 유니크가 엔티티에 안 옮겨졌다는 신호)
-- --- B-3. 무료 → 유료 전환 (결제 트랜잭션) ----------------------------------
-- PAYMENT.id = 1 (PENDING)
INSERT INTO
    payment (
        subscription_id,
        member_id,
        order_id,
        idempotency_key,
        amount,
        status,
        created_at
    )
VALUES
    (
        1,
        2,
        'ORDER-0001',
        'IDEMP-0001',
        15000,
        'PENDING',
        now()
    );

-- PG 콜백 성공 → 같은 트랜잭션으로 묶어서 실행
UPDATE payment
SET
    status = 'SUCCESS',
    pg_transaction_id = 'TOSS-TXN-0001',
    payment_method = 'CARD',
    paid_at = now()
WHERE
    id = 1;

UPDATE subscription
SET
    subscription_level = 'PAID',
    status = 'ACTIVE',
    subscription_price_snapshot = 15000,
    current_period_start_at = now(),
    current_period_end_at = now() + INTERVAL '30' DAY,
    updated_at = now()
WHERE
    id = 1;

-- 확인
SELECT
    s.subscription_level,
    s.status,
    s.subscription_price_snapshot,
    p.status AS payment_status
FROM
    subscription s
    JOIN payment p ON p.subscription_id = s.id
WHERE
    s.id = 1;

-- --- B-4. 결제 멱등성 - 동일 요청 재시도 ------------------------------------
INSERT INTO
    payment (
        subscription_id,
        member_id,
        order_id,
        idempotency_key,
        amount,
        status,
        created_at
    )
VALUES
    (
        1,
        2,
        'ORDER-0001',
        'IDEMP-0001',
        15000,
        'PENDING',
        now()
    );

-- 기대: order_id/idempotency_key UNIQUE 위반으로 에러
INSERT INTO
    payment (
        subscription_id,
        member_id,
        order_id,
        pg_transaction_id,
        idempotency_key,
        amount,
        status,
        created_at
    )
VALUES
    (
        1,
        2,
        'ORDER-9999',
        'TOSS-TXN-0001',
        'IDEMP-9999',
        15000,
        'PENDING',
        now()
    );

-- 기대: pg_transaction_id UNIQUE 위반으로 에러
-- --- B-5. 결제 실패 처리 ----------------------------------------------------
-- PAYMENT.id = 2 (다른 order_id/idempotency_key로 신규 시도, 실패 케이스)
INSERT INTO
    payment (
        subscription_id,
        member_id,
        order_id,
        idempotency_key,
        amount,
        status,
        created_at
    )
VALUES
    (
        1,
        2,
        'ORDER-0002',
        'IDEMP-0002',
        15000,
        'PENDING',
        now()
    );

UPDATE payment
SET
    status = 'FAILED',
    failure_code = 'CARD_DECLINED',
    failure_message = '한도 초과'
WHERE
    id = 2;

-- 확인: SUBSCRIPTION은 그대로 PAID/ACTIVE 유지되는지 (B-3 상태 그대로여야 함)
SELECT
    subscription_level,
    status
FROM
    subscription
WHERE
    id = 1;

-- --- B-6. 유료 혜택 3종 접근 (유료 피드 / 게시판 / 채팅) --------------------
-- "활성 유료 구독자" 판별 쿼리 (이후 쿼리 테스트에서 재사용될 형태)
SELECT
    *
FROM
    subscription
WHERE
    subscriber_id = 2
    AND creator_id = 1
    AND subscription_level = 'PAID'
    AND status = 'ACTIVE';

-- BOARD_POST.id = 1
INSERT INTO
    board_post (
        creator_id,
        member_id,
        title,
        type,
        content,
        status,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        2,
        '배색 관련 질문있어요',
        'QUESTION',
        '2번째 단에서 막혀요',
        'WAITING',
        false,
        now(),
        now()
    );

-- CHAT_ROOM.id = 1
INSERT INTO
    chat_room (
        creator_id,
        member_id,
        status,
        created_at,
        updated_at
    )
VALUES
    (1, 2, 'ACTIVE', now(), now());

-- --- B-7. 구독 취소 예약 → 이용기간 유지 → 만료 후 무료 전환 ---------------
-- 취소 예약
UPDATE subscription
SET
    status = 'CANCEL_SCHEDULED',
    cancel_scheduled_at = now(),
    auto_renew = false,
    updated_at = now()
WHERE
    id = 1;

-- 확인: 예약 상태에서도 current_period_end_at까지는 PAID 혜택 유지되는 값 그대로인지
SELECT
    subscription_level,
    status,
    current_period_end_at
FROM
    subscription
WHERE
    id = 1;

-- (기간 만료 시점 도래를 가정한) 배치 처리 시뮬레이션 → 무료 전환
UPDATE subscription
SET
    subscription_level = 'FREE',
    status = 'ACTIVE',
    subscription_price_snapshot = NULL,
    current_period_start_at = NULL,
    current_period_end_at = NULL,
    cancel_scheduled_at = NULL,
    updated_at = now()
WHERE
    id = 1;

-- --- B-8. 재구독 (만료 후 다시 유료 신청) -----------------------------------
-- PAYMENT.id = 3
INSERT INTO
    payment (
        subscription_id,
        member_id,
        order_id,
        idempotency_key,
        amount,
        status,
        created_at
    )
VALUES
    (
        1,
        2,
        'ORDER-0003',
        'IDEMP-0003',
        15000,
        'PENDING',
        now()
    );

UPDATE payment
SET
    status = 'SUCCESS',
    pg_transaction_id = 'TOSS-TXN-0003',
    payment_method = 'CARD',
    paid_at = now()
WHERE
    id = 3;

-- 기존 subscription.id=1 로우를 UPDATE로 재사용 (신규 INSERT 아님 — UNIQUE 제약 때문)
UPDATE subscription
SET
    subscription_level = 'PAID',
    status = 'ACTIVE',
    subscription_price_snapshot = 15000,
    current_period_start_at = now(),
    current_period_end_at = now() + INTERVAL '30' DAY,
    updated_at = now()
WHERE
    id = 1;

-- --- B-9. 비구독자 투표 차단 (member_id = 3, feed_id = 4의 poll) -----------
-- member_id=3은 SUBSCRIPTION 로우 자체가 없음 → 앱에서 이 SELECT로 차단 판단
SELECT
    *
FROM
    subscription
WHERE
    subscriber_id = 3
    AND creator_id = 1;

-- 결과 0건이면 앱에서 투표 차단 처리 (DB 제약으로 강제되진 않음 — 아래 INSERT는 DB 레벨에선 성공해버림)
INSERT INTO
    poll_vote (
        poll_id,
        poll_option_id,
        member_id,
        weight,
        subscription_level_snapshot,
        created_at,
        updated_at
    )
VALUES
    (1, 2, 3, 1, 'FREE', now(), now());

-- 성공하면: 비구독자 차단이 순전히 앱 책임이라는 점 재확인 (원래 예상대로임, 이슈 아님)
-- --- B-10. 유료 게시판 접근 + 만료 후 제한 ----------------------------------
-- 구독이 다시 PAID/ACTIVE인 상태(B-8 이후)이므로 정상 작성 가능
INSERT INTO
    board_post (
        creator_id,
        member_id,
        title,
        type,
        content,
        status,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        2,
        '재구독 후 질문',
        'FEEDBACK',
        '이 작품 어떤가요',
        'WAITING',
        false,
        now(),
        now()
    );

-- 확인 포인트: board_post에는 "작성 당시 구독자였다"는 스냅샷 컬럼이 없음
-- → 나중에 구독이 만료돼도 이 글이 여전히 존재한다는 사실만으로는
--   "만료 후에도 이 글에 접근 허용해도 되는지"를 판단할 근거가 스키마에 없다 (DECISION-007 미확정과 연결)
-- --- B-11. 1:1 채팅방 생성 제한 + 만료 후 조회만 ----------------------------
-- 확인용 부정 케이스: 동일 (creator_id, member_id) 채팅방 재생성 시도
INSERT INTO
    chat_room (
        creator_id,
        member_id,
        status,
        created_at,
        updated_at
    )
VALUES
    (1, 2, 'ACTIVE', now(), now());

-- 기대: UNIQUE(creator_id, member_id) 위반으로 에러
-- CHAT_MESSAGE.id = 1 (정상 전송)
INSERT INTO
    chat_message (chat_room_id, sender_id, type, content, sent_at)
VALUES
    (1, 2, 'TEXT', '안녕하세요! 질문 있어요', now());

-- 만료 시 전송 차단 판단 쿼리 (CHAT_ROOM 자체엔 구독 상태가 없어 매번 JOIN 필요)
SELECT
    s.subscription_level,
    s.status
FROM
    chat_room cr
    JOIN subscription s ON s.subscriber_id = cr.member_id
    AND s.creator_id = cr.creator_id
WHERE
    cr.id = 1;

-- --- B-12. 결제-구독 트랜잭션 원자성 ----------------------------------------
-- 순수 SQL로는 "하나의 트랜잭션" 여부를 증명할 수 없음 — @Transactional 코드 레벨 테스트 필요.
-- 대신 여기서는 상태 불일치가 실제로 발생 가능한 조합인지만 확인하는 조회:
-- (PAYMENT는 SUCCESS인데 SUBSCRIPTION은 여전히 FREE인 상태가 스키마상 허용되는가?)
SELECT
    p.id AS payment_id,
    p.status AS payment_status,
    s.subscription_level,
    s.status AS sub_status
FROM
    payment p
    JOIN subscription s ON s.id = p.subscription_id
WHERE
    p.status = 'SUCCESS'
    AND s.subscription_level = 'FREE';

-- 이런 조합이 스키마 제약으로 원천 차단되지 않는다 → 트랜잭션 경계(앱 코드)가 유일한 방어선임을 재확인
-- ============================================================================
-- 시나리오 테스트 — 보강안 추가·수정분 (v0.1)
-- 기준: claude/13_API 보강안 (화면 대조 결정 반영) | 선행: 11_시나리오_테스트_검증쿼리.sql
--
-- 전제: 스키마(DDL) 변경 없음. 따라서 11번 시나리오 테스트의 트랜잭션 흐름은 그대로 유효하고
--       재수행 불필요. 이 파일은 보강안이 "새로 정의한 API"가 유발하는 write 흐름 중
--       11번에서 다루지 않았던 것 + 결정사항으로 표현이 바뀐 것만 추가로 메모한다.
--       데이터는 11번 스크립트로 만든 상태(member 1=크리에이터, 2=구독자, 3=비구독자,
--       feed 1~4, project 1~2, subscription 1, board_post 1~2 등)를 그대로 이어서 사용.
-- ============================================================================
-- ============================================================================
-- [추가] AS. 프로젝트 리소스 (보강안 P-3 ~ P-5)
--   11번에서는 프로젝트를 setup 데이터로만 INSERT했고, 리소스로서의 생성/수정/삭제
--   흐름은 검증하지 않았다. 보강안에서 PROJECT API를 신설했으므로 아래를 추가한다.
-- ============================================================================
-- --- AS-1. 프로젝트 생성 — 비활성 카테고리 차단 (P-3) --------------------------
-- 활성 카테고리로 정상 생성 (PROJECT.id = 3)
INSERT INTO
    project (
        creator_id,
        category_id,
        title,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (1, 1, '여름 코스터 시리즈', false, now(), now());

-- 비활성 카테고리 준비
INSERT INTO
    category (name, is_active, created_at, updated_at)
VALUES
    ('폐지예정공예', false, now(), now());

-- 비활성 카테고리(id = 3 가정)로 프로젝트 생성 시도
INSERT INTO
    project (
        creator_id,
        category_id,
        title,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (1, 3, '비활성 카테고리 프로젝트', false, now(), now());

-- 확인 포인트: 이 INSERT는 DB 레벨에선 성공해버린다 (category.is_active를 FK가 검사하지 않음).
-- "활성화된 카테고리만 선택 가능" 정책은 P-3 요청 검증(앱 레이어)에서만 막힌다 →
-- 400 INVALID_REQUEST로 처리해야 하는 지점. 시나리오 테스트 결론: 스키마가 아니라 서비스 책임.
-- (정리) DELETE FROM project WHERE title = '비활성 카테고리 프로젝트';
-- --- AS-2. 프로젝트 카테고리 변경 → 소속 피드 자동 반영 (P-4) ------------------
-- project 1(뜨개질, category_id=1)의 카테고리를 자수(category_id=2)로 변경
UPDATE project
SET
    category_id = 2,
    updated_at = now()
WHERE
    id = 1;

-- 확인 포인트: FEED에는 category 컬럼이 없으므로, project 1에 속한 모든 피드가
-- 별도 UPDATE 없이 자동으로 '자수'로 조회된다 (11번 A-5에서 검증한 상속 구조의 재확인).
SELECT
    f.id,
    f.title,
    c.name AS category_now
FROM
    feed f
    JOIN project p ON p.id = f.project_id
    JOIN category c ON c.id = p.category_id
WHERE
    f.project_id = 1;

-- (정리) UPDATE project SET category_id = 1 WHERE id = 1;  -- 원복
-- --- AS-3. 프로젝트 삭제 차단 재확인 (P-5) ------------------------------------
-- 이미 11번 A-7에서 검증함. 보강안에서 이 정책에 전용 에러코드(PROJECT_HAS_ACTIVE_FEEDS, 409)를
-- 부여했으므로, "활성 피드 존재 → 삭제 시도 → 앱이 409 반환" 흐름을 서비스 테스트로 이관한다는
-- 점만 메모. DB 제약이 없다는 사실은 그대로(11번 A-7 결론 유지).
-- ============================================================================
-- [추가] BS. 유료 게시판 게시글 상태 전이 (보강안 B-2 ~ B-4)
--   11번 B-6/B-10에서는 board_post INSERT까지만 했다. 보강안이 게시글 목록/작성/상세/수정
--   API를 신설했으므로, 상태 전이(WAITING→ANSWERED)와 수정 제한을 추가로 검증한다.
-- ============================================================================
-- --- BS-1. 게시글 작성 → 공식 답변 등록 → 상태 전이 --------------------------
-- board_post 1은 11번 B-6에서 status='WAITING'로 이미 존재. 여기에 공식 답변을 단다.
-- BOARD_ANSWER.id = 1 (BOARD_POST 1—0..1 BOARD_ANSWER)
INSERT INTO
    board_answer (
        board_post_id,
        creator_id,
        content,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (
        1,
        1,
        '2단 시작 코를 하나 줄여보세요. 사진상 장력이 높습니다.',
        false,
        now(),
        now()
    );

-- 답변 등록과 같은 트랜잭션에서 게시글 상태를 ANSWERED로 전이 (B-3 상세 응답의 status 필드 대응)
UPDATE board_post
SET
    status = 'ANSWERED',
    updated_at = now()
WHERE
    id = 1;

-- 확인
SELECT
    bp.id,
    bp.status,
    ba.id AS answer_id
FROM
    board_post bp
    LEFT JOIN board_answer ba ON ba.board_post_id = bp.id
WHERE
    bp.id = 1;

-- --- BS-2. 게시글당 공식 답변 1건 제한 --------------------------------------
-- 동일 board_post에 답변 2건째 시도
INSERT INTO
    board_answer (
        board_post_id,
        creator_id,
        content,
        is_deleted,
        created_at,
        updated_at
    )
VALUES
    (1, 1, '두 번째 답변 시도', false, now(), now());

-- 기대: UNIQUE(board_post_id) 위반으로 에러 (BOARD_ANSWER 제약). 막히면 정상.
-- --- BS-3. 답변 등록 후 게시글 수정 차단 (B-4, DECISION-006 기본안) ----------
-- 이미 status='ANSWERED'인 board_post 1의 본문 수정 시도
UPDATE board_post
SET
    content = '답변 후 수정 시도',
    updated_at = now()
WHERE
    id = 1;

-- 확인 포인트: 이 UPDATE는 DB에선 성공해버린다. "답변 전까지만 수정 허용" 정책은
-- B-4 요청 검증(앱 레이어)에서 status를 보고 409로 막아야 하는 지점 → 서비스 책임.
-- (정리) 원복 필요 시 content 되돌리기.
-- ============================================================================
-- [추가] MS. 회원 프로필 수정 (보강안 M-2)
--   보강안이 PATCH /api/members/me를 신설. 닉네임 UNIQUE 충돌만 write 흐름으로 확인.
-- ============================================================================
-- --- MS-1. 닉네임 변경 정상 ----------------------------------------------------
UPDATE member
SET
    nickname = '지수공방',
    updated_at = now()
WHERE
    id = 2;

-- --- MS-2. 닉네임 중복 차단 ----------------------------------------------------
-- member 1의 닉네임('민호')으로 member 2 변경 시도
UPDATE member
SET
    nickname = '민호',
    updated_at = now()
WHERE
    id = 2;

-- 기대: UNIQUE(nickname) 위반으로 에러. 막히면 정상 (M-2 응답의 409 DUPLICATE_RESOURCE 근거).
-- (정리) UPDATE member SET nickname = '지수' WHERE id = 2;
-- ============================================================================
-- [수정 없음 확인] 결정사항이 스키마/write 흐름에 준 영향 점검
-- ============================================================================
-- · bio 삭제: 11번 setup의 member INSERT는 애초에 bio 컬럼을 쓰지 않았다 → 변경 없음.
-- · POLL 가중치 확정: 11번 A-11/B-9의 poll_vote INSERT(weight, subscription_level_snapshot)가
--   이미 가중치 방식이었다 → 변경 없음.
-- · 환불/약관/카드번호/태그: 전부 화면 또는 문서 정정이라 write 흐름 영향 없음 → 변경 없음.
-- · PROJECT stage: MVP 제외(보류)라 이번 시나리오 테스트 대상 아님. 이후 컬럼 추가 시
--   feed INSERT에 stage 값을 넣는 스텝 1개만 추가하면 되고 기존 흐름은 유효.
--
-- 결론: 스키마 무변경이 확정이므로 11번 시나리오 테스트 재수행은 불필요하고,
--       위 AS/BS/MS 흐름만 추가로 한 번 돌려보면 보강안이 유발한 write 경로까지 커버된다.