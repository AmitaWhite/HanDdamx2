ALTER TABLE creator_application ADD COLUMN IF NOT EXISTS introduction VARCHAR(1000);
ALTER TABLE creator_application ADD COLUMN IF NOT EXISTS representative_image_url VARCHAR(500);
