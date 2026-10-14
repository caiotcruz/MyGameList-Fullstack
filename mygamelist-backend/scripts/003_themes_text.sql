BEGIN;
ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS theme varchar(16) NOT NULL DEFAULT 'BLUE';
ALTER TABLE tb_users ALTER COLUMN bio TYPE text;
ALTER TABLE tb_user_game_list ALTER COLUMN review TYPE text;
ALTER TABLE tb_activities ALTER COLUMN detail TYPE text;
ALTER TABLE tb_comments ALTER COLUMN text TYPE text;
ALTER TABLE tb_messages ALTER COLUMN content TYPE text;
COMMIT;
