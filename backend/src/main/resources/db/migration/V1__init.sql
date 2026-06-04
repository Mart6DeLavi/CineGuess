CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id uuid PRIMARY KEY,
    username varchar(100) NOT NULL UNIQUE,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE movies (
    id bigserial PRIMARY KEY,
    tmdb_id bigint NOT NULL UNIQUE,
    title varchar(255) NOT NULL,
    original_title varchar(255),
    release_date date,
    poster_path varchar(255),
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE movie_videos (
    id uuid PRIMARY KEY,
    movie_id bigint NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
    youtube_key varchar(100) NOT NULL,
    type varchar(50) NOT NULL,
    official boolean NOT NULL DEFAULT false,
    duration_seconds int,
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_movie_video_youtube_key UNIQUE (movie_id, youtube_key)
);

CREATE TABLE daily_challenges (
    id uuid PRIMARY KEY,
    challenge_date date NOT NULL UNIQUE,
    movie_id bigint NOT NULL REFERENCES movies(id),
    movie_video_id uuid NOT NULL REFERENCES movie_videos(id),
    fragment_start int NOT NULL,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE attempts (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES users(id),
    challenge_id uuid NOT NULL REFERENCES daily_challenges(id),
    answer varchar(255) NOT NULL,
    stage_seconds int NOT NULL,
    correct boolean NOT NULL,
    score int NOT NULL,
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX idx_movie_videos_movie_id ON movie_videos(movie_id);
CREATE INDEX idx_attempts_challenge_score ON attempts(challenge_id, score DESC);
CREATE INDEX idx_daily_challenges_date ON daily_challenges(challenge_date);
