BEGIN;
-- Abort on case-folded account collisions; never merge/delete accounts automatically.
DO $$ BEGIN
 IF EXISTS(SELECT lower(trim(email)) FROM tb_users GROUP BY lower(trim(email)) HAVING count(*)>1)
 THEN RAISE EXCEPTION 'Duplicate normalized email addresses: resolve them before migration'; END IF;
END $$;
UPDATE tb_users SET email=lower(trim(email)), token_version=token_version+1 WHERE email<>lower(trim(email));
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized ON tb_users(lower(email));
-- Plain six-digit codes from older releases expire once; new codes are keyed hashes.
UPDATE tb_users SET verification_code=NULL,verification_expiry=NULL WHERE verification_code ~ '^[0-9]{6}$';
CREATE INDEX IF NOT EXISTS idx_messages_conversation_id ON tb_messages(conversation_id,id DESC);
CREATE INDEX IF NOT EXISTS idx_user_game_owner ON tb_user_game_list(user_id);
CREATE INDEX IF NOT EXISTS idx_review_game_recent ON tb_user_game_list(game_id,updated_at DESC) WHERE review IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_pending_genres ON tb_games(id) WHERE genre_slugs IS NULL;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname='ck_users_nonnegative_progress') THEN
  ALTER TABLE tb_users ADD CONSTRAINT ck_users_nonnegative_progress CHECK (experience>=0 AND level>=1) NOT VALID;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname='ck_library_score') THEN
  ALTER TABLE tb_user_game_list ADD CONSTRAINT ck_library_score CHECK (score IS NULL OR score BETWEEN 0 AND 10) NOT VALID;
 END IF;
END $$;
COMMIT;
