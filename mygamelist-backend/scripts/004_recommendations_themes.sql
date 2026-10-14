BEGIN;
ALTER TABLE tb_users ALTER COLUMN theme SET DEFAULT 'TEAL';
ALTER TABLE tb_users DROP CONSTRAINT IF EXISTS tb_users_theme_check;
ALTER TABLE tb_users ADD CONSTRAINT tb_users_theme_check CHECK (theme IN ('BLUE','TEAL','AMBER','ROSE','PURPLE','TWILIGHT','DAWN','OCEAN','HACKER'));
ALTER TABLE tb_games ADD COLUMN IF NOT EXISTS genre_slugs text;
ALTER TABLE tb_games ADD COLUMN IF NOT EXISTS genre_retry_at timestamp;
COMMIT;
