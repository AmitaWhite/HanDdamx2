-- 이미 구 스키마(V1/V2)가 적용된 로컬 DB를 최신 ERD에 맞춘다.
ALTER TABLE board_post
    ADD COLUMN IF NOT EXISTS title VARCHAR(255);

UPDATE board_post
SET title = LEFT(content, 255)
WHERE title IS NULL OR title = '';

ALTER TABLE board_post
    ALTER COLUMN title SET NOT NULL;

ALTER TABLE board_post
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

ALTER TABLE board_post_image
    ADD COLUMN IF NOT EXISTS file_size BIGINT;

ALTER TABLE board_post_image
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100);

ALTER TABLE board_post_image
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;

UPDATE board_post_image
SET created_at = NOW()
WHERE created_at IS NULL;

ALTER TABLE board_post_image
    ALTER COLUMN created_at SET NOT NULL;

UPDATE board_post_image
SET storage_key = CONCAT('legacy/', id)
WHERE storage_key IS NULL OR storage_key = '';

ALTER TABLE board_post_image
    ALTER COLUMN storage_key SET NOT NULL;
