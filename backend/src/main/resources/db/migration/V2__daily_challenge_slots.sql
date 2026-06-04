ALTER TABLE daily_challenges
    ADD COLUMN IF NOT EXISTS challenge_slot int NOT NULL DEFAULT 0;

ALTER TABLE daily_challenges
    DROP CONSTRAINT IF EXISTS daily_challenges_challenge_date_key;

ALTER TABLE daily_challenges
    DROP CONSTRAINT IF EXISTS uq_daily_challenges_date_slot;

ALTER TABLE daily_challenges
    ADD CONSTRAINT uq_daily_challenges_date_slot UNIQUE (challenge_date, challenge_slot);
