CREATE TABLE board_post (
    id              BIGSERIAL PRIMARY KEY,
    creator_id      BIGINT NOT NULL,
    member_id       BIGINT NOT NULL,
    type            VARCHAR(50) NOT NULL,
    content         TEXT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'WAITING',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_board_post_creator_deleted_created
    ON board_post (creator_id, is_deleted, created_at DESC);
