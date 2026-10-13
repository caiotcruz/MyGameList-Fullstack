BEGIN;
CREATE TABLE IF NOT EXISTS mail_outbox (
 id varchar(36) PRIMARY KEY,
 recipient text NOT NULL,
 subject text NOT NULL,
 body text NOT NULL,
 state varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (state IN ('PENDING','SENT','FAILED','EXPIRED')),
 attempts integer NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 expires_at timestamptz NOT NULL,
 next_attempt_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_mail_outbox_due ON mail_outbox(next_attempt_at) WHERE state='PENDING';
CREATE INDEX IF NOT EXISTS idx_mail_outbox_created ON mail_outbox(created_at);
COMMIT;
