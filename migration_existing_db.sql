-- Backup an existing PostgreSQL database before applying this script.
ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS reset_token_hash varchar(255);
ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS reset_token_expiry timestamp;
ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS token_version integer NOT NULL DEFAULT 0;
CREATE INDEX IF NOT EXISTS idx_activities_user_timestamp ON tb_activities (user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_activities_group ON tb_activities (group_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON tb_notifications (user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_messages_conversation_sent ON tb_messages (conversation_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_user_game_game_updated ON tb_user_game_list (game_id, updated_at DESC);
