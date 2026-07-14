-- ============================================================================
-- 한땀한땀 로컬 개발용 샘플 데이터 시드 (v1)
-- 대상: local 프로필 Postgres (handdam DB). test(H2) 아님.
-- 기준 스키마: V202607122313__init_schema.sql (실제 마이그레이션 그대로 대조 완료)
--
-- ▶ 사용법
--   1) dbcode 등에서 handdam DB에 접속해 이 파일 전체를 한 번 실행한다.
--   2) 다시 실행해도 안전하다 — 모든 INSERT가 고정 id + ON CONFLICT DO NOTHING이라
--      "비어 있으면 넣고, 이미 있으면 건너뜀"으로 동작한다. (= 중복 삽입 없음)
--   3) 이미 각자 수동으로 member 등을 넣어 id가 꼬였다면, 아래 [RESET] 블록 주석을 풀어
--      한 번 비우고 다시 실행하는 걸 권장(팀 공통 baseline 정렬).
--
-- ▶ 팀 공유용 고정 member id (지금 쓰는 x-member-id 헤더에 그대로 사용)
--   1 = 민호(민호공방)   CREATOR   — 피드/게시판/채팅 소유자
--   2 = 지수             USER      — 민호 PAID 구독자 (+서연 PAID) : 유료 기능 전반 테스트
--   3 = 하늘             USER      — 민호 FREE 구독자             : 무료 등급 테스트
--   4 = 관리자           ADMIN     — 크리에이터 승인/카테고리 관리
--   5 = 서연             CREATOR   — 두 번째 크리에이터
--   6 = 도윤             USER      — 비구독자                     : 잠금/차단 테스트
--   7 = 구글가입자       USER      — OAuth(GOOGLE), password NULL : 소셜 로그인 경로
--   8 = 유나             USER      — 서연 FREE 구독자 + 크리에이터 신청 PENDING
--
-- ▶ 비밀번호: 전원 평문 "test1234" (BCrypt $2b$10$ 해시로 저장 — 로그인 붙으면 그대로 검증됨)
--   OAuth 가입자(7)는 password NULL (스키마 ck_member_auth_source 규칙).
-- ============================================================================
BEGIN;

-- ─────────────────────────────────────────────────────────────────────────
-- [RESET] 필요 시에만 주석 해제 (기존 데이터 전부 삭제 후 재시드)
-- TRUNCATE notification, chat_message, chat_room, board_comment, board_answer,
--   board_post_image, board_post, payment, subscription, poll_vote, poll_option,
--   poll, feed_like, feed_comment, feed_attachment, feed, project, category,
--   creator_application, creator_profile, email_verification, member
--   RESTART IDENTITY CASCADE;
-- ─────────────────────────────────────────────────────────────────────────
-- 1. MEMBER ------------------------------------------------------------------
INSERT INTO
    member (
        id,
        email,
        password,
        nickname,
        role,
        oauth_provider,
        oauth_id,
        email_verified_at
    )
VALUES
    (
        1,
        'minho@handdam.dev',
        '$2b$10$R3cLlYMQVoraNKLE.L1BEODzBXnZqATPtJ9AQlWbn0/Z1MXomkYvS',
        '민호공방',
        'CREATOR',
        'NONE',
        NULL,
        now() - INTERVAL '40 days'
    ),
    (
        2,
        'jisu@handdam.dev',
        '$2b$10$R3cLlYMQVoraNKLE.L1BEODzBXnZqATPtJ9AQlWbn0/Z1MXomkYvS',
        '지수',
        'USER',
        'NONE',
        NULL,
        now() - INTERVAL '30 days'
    ),
    (
        3,
        'haneul@handdam.dev',
        '$2b$10$R3cLlYMQVoraNKLE.L1BEODzBXnZqATPtJ9AQlWbn0/Z1MXomkYvS',
        '하늘',
        'USER',
        'NONE',
        NULL,
        now() - INTERVAL '25 days'
    ),
    (
        4,
        'admin@handdam.dev',
        '$2b$10$R3cLlYMQVoraNKLE.L1BEODzBXnZqATPtJ9AQlWbn0/Z1MXomkYvS',
        '관리자',
        'ADMIN',
        'NONE',
        NULL,
        now() - INTERVAL '50 days'
    ),
    (
        5,
        'seoyeon@handdam.dev',
        '$2b$10$R3cLlYMQVoraNKLE.L1BEODzBXnZqATPtJ9AQlWbn0/Z1MXomkYvS',
        '서연',
        'CREATOR',
        'NONE',
        NULL,
        now() - INTERVAL '35 days'
    ),
    (
        6,
        'doyun@handdam.dev',
        '$2b$10$R3cLlYMQVoraNKLE.L1BEODzBXnZqATPtJ9AQlWbn0/Z1MXomkYvS',
        '도윤',
        'USER',
        'NONE',
        NULL,
        now() - INTERVAL '10 days'
    ),
    (
        7,
        'google@handdam.dev',
        NULL,
        '구글가입자',
        'USER',
        'GOOGLE',
        'google-108407711',
        now() - INTERVAL '5 days'
    ),
    (
        8,
        'yuna@handdam.dev',
        '$2b$10$R3cLlYMQVoraNKLE.L1BEODzBXnZqATPtJ9AQlWbn0/Z1MXomkYvS',
        '유나',
        'USER',
        'NONE',
        NULL,
        NULL
    ) -- email 미인증 케이스
ON CONFLICT (id) DO NOTHING;

-- 2. CREATOR_PROFILE ---------------------------------------------------------
INSERT INTO
    creator_profile (
        id,
        member_id,
        introduction,
        benefits_description,
        subscription_price,
        representative_image_url
    )
VALUES
    (
        1,
        1,
        '뜨개질 10년차, 겨울 소품 위주로 작업합니다.',
        '유료 구독 시: 상세 도안 PDF, 전용 Q&A 게시판, 1:1 채팅 제공',
        9900,
        'https://picsum.photos/seed/minho/400'
    ),
    (
        2,
        5,
        '자수와 비즈공예 작업하는 서연입니다.',
        '유료 구독 시: 고화질 도안, 재료 리스트, 1:1 상담',
        5000,
        'https://picsum.photos/seed/seoyeon/400'
    )
ON CONFLICT (id) DO NOTHING;

-- 3. CREATOR_APPLICATION -----------------------------------------------------
INSERT INTO
    creator_application (id, member_id, status, reviewed_by, reviewed_at)
VALUES
    (1, 8, 'PENDING', NULL, NULL), -- 유나: 심사 대기 (uq_creator_application_pending 검증용)
    (2, 1, 'APPROVED', 4, now() - INTERVAL '40 days') -- 민호: 과거 승인 이력
ON CONFLICT (id) DO NOTHING;

-- 4. CATEGORY ----------------------------------------------------------------
INSERT INTO
    category (id, name, is_active)
VALUES
    (1, '뜨개질', true),
    (2, '자수', true),
    (3, '비즈공예', true),
    (4, '레진공예', true),
    (5, '키링', true)
ON CONFLICT (id) DO NOTHING;

-- 5. PROJECT (피드 폴더) -----------------------------------------------------
INSERT INTO
    project (
        id,
        creator_id,
        category_id,
        title,
        description,
        cover_image_url
    )
VALUES
    (
        1,
        1,
        1,
        '겨울 목도리 프로젝트',
        '기초 코부터 완성까지 단계별 기록',
        'https://picsum.photos/seed/proj1/600'
    ),
    (
        2,
        1,
        2,
        '들꽃 자수 연작',
        '봄 들꽃을 주제로 한 자수 리스 시리즈',
        'https://picsum.photos/seed/proj2/600'
    ),
    (
        3,
        5,
        3,
        '여름 비즈 팔찌',
        '초보용 비즈 팔찌 만들기',
        'https://picsum.photos/seed/proj3/600'
    )
ON CONFLICT (id) DO NOTHING;

-- 6. FEED (comment_count/like_count는 아래 실제 삽입 건수와 일치시켜 둠) ------
INSERT INTO
    feed (
        id,
        project_id,
        title,
        content,
        visibility,
        comment_count,
        like_count,
        created_at
    )
VALUES
    (
        1,
        1,
        '목도리 뜨기 시작합니다',
        '오늘부터 겨울 목도리 시작! 기초 코 잡는 것부터 보여드릴게요.',
        'PUBLIC',
        3,
        2,
        now() - INTERVAL '9 days'
    ),
    (
        2,
        1,
        '1단 배색 고민',
        '무료 구독자분들께 먼저 배색 후보를 공유해요.',
        'FREE_SUBSCRIBER',
        0,
        0,
        now() - INTERVAL '7 days'
    ),
    (
        3,
        1,
        '상세 도안 공개 (유료)',
        '유료 구독자 전용 상세 도안입니다. 투표도 참여해주세요!',
        'PAID_SUBSCRIBER',
        0,
        1,
        now() - INTERVAL '5 days'
    ),
    (
        4,
        2,
        '들꽃 자수 소개',
        '이번 연작의 콘셉트를 소개합니다.',
        'PUBLIC',
        0,
        0,
        now() - INTERVAL '4 days'
    ),
    (
        5,
        2,
        '자수 도안 PDF (유료)',
        '유료 구독자용 고화질 도안 PDF를 첨부했어요.',
        'PAID_SUBSCRIBER',
        0,
        0,
        now() - INTERVAL '3 days'
    ),
    (
        6,
        3,
        '비즈 팔찌 완성작',
        '여름 비즈 팔찌 완성 사진입니다.',
        'PUBLIC',
        0,
        0,
        now() - INTERVAL '2 days'
    )
ON CONFLICT (id) DO NOTHING;

-- 7. FEED_ATTACHMENT ---------------------------------------------------------
INSERT INTO
    feed_attachment (
        id,
        feed_id,
        type,
        url,
        storage_key,
        original_name,
        file_size,
        mime_type,
        order_index
    )
VALUES
    (
        1,
        1,
        'IMAGE',
        'https://picsum.photos/seed/f1a/800',
        'local/f1a.jpg',
        NULL,
        204800,
        'image/jpeg',
        0
    ),
    (
        2,
        3,
        'IMAGE',
        'https://picsum.photos/seed/f3a/800',
        'local/f3a.jpg',
        NULL,
        256000,
        'image/jpeg',
        0
    ),
    (
        3,
        3,
        'FILE',
        'https://cdn.local/도안_목도리.pdf',
        'local/f3.pdf',
        '목도리_도안.pdf',
        1048576,
        'application/pdf',
        1
    ),
    (
        4,
        4,
        'IMAGE',
        'https://picsum.photos/seed/f4a/800',
        'local/f4a.jpg',
        NULL,
        198000,
        'image/jpeg',
        0
    ),
    (
        5,
        5,
        'FILE',
        'https://cdn.local/도안_자수.pdf',
        'local/f5.pdf',
        '자수_도안.pdf',
        2097152,
        'application/pdf',
        0
    )
ON CONFLICT (id) DO NOTHING;

-- 8. POLL / OPTION / VOTE (feed 3에 종속) ------------------------------------
INSERT INTO
    poll (id, feed_id, question, end_at, is_closed)
VALUES
    (
        1,
        3,
        '다음 배색, 어떤 게 좋을까요?',
        now() + INTERVAL '3 days',
        false
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO
    poll_option (id, poll_id, option_text, order_index)
VALUES
    (1, 1, '세이지 그린 그라데이션', 0),
    (2, 1, '딥그린 단색', 1),
    (3, 1, '그린 + 골드 포인트', 2)
ON CONFLICT (id) DO NOTHING;

-- 지수(PAID)=가중치 2, 하늘(FREE)=가중치 1
INSERT INTO
    poll_vote (
        id,
        poll_id,
        poll_option_id,
        member_id,
        weight,
        subscription_level_snapshot
    )
VALUES
    (1, 1, 1, 2, 2, 'PAID'),
    (2, 1, 2, 3, 1, 'FREE')
ON CONFLICT (id) DO NOTHING;

-- 9. FEED_COMMENT (feed 1: 댓글 2 + 대댓글 1 = 3건 → feed.comment_count=3) ----
INSERT INTO
    feed_comment (
        id,
        feed_id,
        member_id,
        parent_comment_id,
        depth,
        content,
        created_at
    )
VALUES
    (
        1,
        1,
        2,
        NULL,
        0,
        '기초 코 잡는 게 늘 어려웠는데 도움돼요!',
        now() - INTERVAL '9 days'
    ),
    (
        2,
        1,
        1,
        1,
        1,
        '천천히 따라오시면 돼요 :)',
        now() - INTERVAL '8 days'
    ),
    (
        3,
        1,
        3,
        NULL,
        0,
        '다음 편 기대할게요~',
        now() - INTERVAL '8 days'
    )
ON CONFLICT (id) DO NOTHING;

-- 10. FEED_LIKE (feed 1: 2건, feed 3: 1건) -----------------------------------
INSERT INTO
    feed_like (id, feed_id, member_id)
VALUES
    (1, 1, 2),
    (2, 1, 3),
    (3, 3, 2)
ON CONFLICT (id) DO NOTHING;

-- 11. SUBSCRIPTION -----------------------------------------------------------
INSERT INTO
    subscription (
        id,
        subscriber_id,
        creator_id,
        subscription_level,
        status,
        subscription_price_snapshot,
        auto_renew,
        started_at,
        current_period_start_at,
        current_period_end_at
    )
VALUES
    (
        1,
        2,
        1,
        'PAID',
        'ACTIVE',
        9900,
        true,
        now() - INTERVAL '20 days',
        now() - INTERVAL '5 days',
        now() + INTERVAL '25 days'
    ), -- 지수→민호 유료
    (
        2,
        3,
        1,
        'FREE',
        'ACTIVE',
        NULL,
        false,
        now() - INTERVAL '15 days',
        NULL,
        NULL
    ), -- 하늘→민호 무료
    (
        3,
        8,
        5,
        'FREE',
        'ACTIVE',
        NULL,
        false,
        now() - INTERVAL '8 days',
        NULL,
        NULL
    ), -- 유나→서연 무료
    (
        4,
        2,
        5,
        'PAID',
        'ACTIVE',
        5000,
        true,
        now() - INTERVAL '6 days',
        now() - INTERVAL '6 days',
        now() + INTERVAL '24 days'
    ) -- 지수→서연 유료
ON CONFLICT (id) DO NOTHING;

-- 12. PAYMENT ----------------------------------------------------------------
INSERT INTO
    payment (
        id,
        subscription_id,
        member_id,
        order_id,
        pg_transaction_id,
        idempotency_key,
        amount,
        status,
        payment_method,
        paid_at,
        created_at
    )
VALUES
    (
        1,
        1,
        2,
        'ORD-20260620-0001',
        'TOSS-TXN-0001',
        'IDEM-0001',
        9900,
        'SUCCESS',
        'CARD',
        now() - INTERVAL '20 days',
        now() - INTERVAL '20 days'
    ),
    (
        2,
        4,
        2,
        'ORD-20260705-0002',
        'TOSS-TXN-0002',
        'IDEM-0002',
        5000,
        'SUCCESS',
        'CARD',
        now() - INTERVAL '6 days',
        now() - INTERVAL '6 days'
    )
ON CONFLICT (id) DO NOTHING;

-- 13. BOARD (민호 게시판, creator_id=1 / 유료 구독자 지수만 작성 가능) --------
INSERT INTO
    board_post (
        id,
        creator_id,
        member_id,
        title,
        type,
        content,
        status,
        created_at
    )
VALUES
    (
        1,
        1,
        2,
        '2단에서 코가 자꾸 풀려요',
        'QUESTION',
        '사진처럼 자꾸 풀리는데 장력 문제일까요?',
        'WAITING',
        now() - INTERVAL '4 days'
    ),
    (
        2,
        1,
        2,
        '완성작 피드백 부탁드려요',
        'FEEDBACK',
        '첫 목도리 완성했어요! 봐주세요.',
        'ANSWERED',
        now() - INTERVAL '3 days'
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO
    board_post_image (
        id,
        board_post_id,
        url,
        storage_key,
        original_name,
        order_index
    )
VALUES
    (
        1,
        1,
        'https://picsum.photos/seed/bp1/600',
        'local/bp1.jpg',
        '문제상황.jpg',
        0
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO
    board_answer (id, board_post_id, creator_id, content)
VALUES
    (1, 2, 1, '정말 잘 만드셨어요! 가장자리만 한 코 더 촘촘히 하면 완벽합니다.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO
    board_comment (
        id,
        board_post_id,
        member_id,
        parent_comment_id,
        depth,
        content
    )
VALUES
    (1, 2, 2, NULL, 0, '답변 감사합니다! 다음엔 그렇게 해볼게요.')
ON CONFLICT (id) DO NOTHING;

-- 14. CHAT (민호 1 ↔ 지수 2) -------------------------------------------------
INSERT INTO
    chat_room (
        id,
        creator_id,
        member_id,
        status,
        last_message_at
    )
VALUES
    (1, 1, 2, 'ACTIVE', now() - INTERVAL '1 day')
ON CONFLICT (id) DO NOTHING;

INSERT INTO
    chat_message (
        id,
        chat_room_id,
        sender_id,
        type,
        content,
        image_url,
        read_at,
        sent_at
    )
VALUES
    (
        1,
        1,
        2,
        'TEXT',
        '작가님 도안 관련 질문 있어요!',
        NULL,
        now() - INTERVAL '1 day',
        now() - INTERVAL '1 day' - INTERVAL '10 min'
    ),
    (
        2,
        1,
        1,
        'TEXT',
        '네 편하게 말씀하세요 :)',
        NULL,
        now() - INTERVAL '1 day',
        now() - INTERVAL '1 day' - INTERVAL '5 min'
    ),
    (
        3,
        1,
        2,
        'IMAGE',
        NULL,
        'https://picsum.photos/seed/chat3/600',
        NULL,
        now() - INTERVAL '1 day'
    ) -- 아직 안읽음
ON CONFLICT (id) DO NOTHING;

-- 15. NOTIFICATION -----------------------------------------------------------
INSERT INTO
    notification (
        id,
        member_id,
        sender_id,
        type,
        message,
        reference_id,
        reference_type,
        is_read
    )
VALUES
    (
        1,
        1,
        2,
        'FEED_COMMENT',
        '지수님이 회원님의 피드에 댓글을 남겼습니다.',
        1,
        'FEED_COMMENT',
        false
    ),
    (
        2,
        2,
        1,
        'BOARD_ANSWER',
        '민호공방님이 회원님의 질문에 답변했습니다.',
        2,
        'BOARD_POST',
        false
    ),
    (
        3,
        2,
        NULL,
        'PAYMENT_SUCCESS',
        '서연님 유료 구독 결제가 완료되었습니다.',
        2,
        'PAYMENT',
        true
    )
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- [필수] IDENTITY 시퀀스 재동기화
--   고정 id로 넣었기 때문에 시퀀스는 여전히 1을 가리킨다. 이 재동기화를 안 하면
--   이후 앱이 새 row를 INSERT할 때 id=1부터 다시 시도해 PK 충돌이 난다.
-- ============================================================================
SELECT
    setval(
        pg_get_serial_sequence('member', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                member
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('creator_profile', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                creator_profile
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('creator_application', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                creator_application
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('category', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                category
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('project', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                project
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('feed', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                feed
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('feed_attachment', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                feed_attachment
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('poll', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                poll
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('poll_option', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                poll_option
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('poll_vote', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                poll_vote
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('feed_comment', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                feed_comment
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('feed_like', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                feed_like
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('subscription', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                subscription
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('payment', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                payment
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('board_post', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                board_post
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('board_post_image', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                board_post_image
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('board_answer', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                board_answer
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('board_comment', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                board_comment
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('chat_room', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                chat_room
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('chat_message', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                chat_message
        )
    );

SELECT
    setval(
        pg_get_serial_sequence('notification', 'id'),
        (
            SELECT
                COALESCE(MAX(id), 1)
            FROM
                notification
        )
    );

COMMIT;

-- 확인용 (선택): 각 테이블 건수
-- SELECT 'member' t, count(*) FROM member UNION ALL SELECT 'feed', count(*) FROM feed
-- UNION ALL SELECT 'subscription', count(*) FROM subscription ORDER BY 1;
-- SELECT COUNT(*) as member_count FROM member;
-- SELECT COUNT(*) as feed_count FROM feed;
-- SELECT COUNT(*) as subscription_count FROM subscription;
-- -- 시퀀스 확인:
-- SELECT last_value FROM member_id_seq;
-- SELECT last_value FROM feed_id_seq;