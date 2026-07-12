CREATE TABLE board_post_image (
    id              BIGSERIAL PRIMARY KEY,
    board_post_id   BIGINT NOT NULL,
    url             VARCHAR(500) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    original_name   VARCHAR(255),
    file_size       BIGINT,
    mime_type       VARCHAR(100),
    order_index     INT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_board_post_image_post
        FOREIGN KEY (board_post_id) REFERENCES board_post (id)
);

CREATE INDEX idx_board_post_image_post_order
    ON board_post_image (board_post_id, order_index);
