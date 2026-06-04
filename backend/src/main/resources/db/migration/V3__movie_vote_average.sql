ALTER TABLE movies
    ADD COLUMN IF NOT EXISTS vote_average double precision;
