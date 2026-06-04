# Project: CineGuess

Сделай fullstack-платформу для угадывания фильмов по коротким видеофрагментам.

## Tech Stack

Backend:
- Java 21
- Reactive Spring Boot WebFlux
- Spring Data R2DBC
- PostgreSQL
- Redis
- Flyway
- Docker Compose

Frontend:
- React
- TypeScript
- Vite
- TailwindCSS
- YouTube IFrame Player API

## Главная идея

Пользователь заходит в режим Daily Challenge. Ему показывается короткий случайный фрагмент из официального трейлера или клипа фильма. Пользователь должен угадать фильм.

Фрагменты открываются по этапам:

- 1 секунда
- 4 секунды
- 15 секунд
- 30 секунд
- 60 секунд

Чем раньше пользователь угадал фильм, тем больше очков получает.

## Источник данных

Использовать TMDb API.

Backend должен подготовить интеграцию:

- получить популярные фильмы
- получить данные фильма
- получить список видео фильма через `/movie/{movie_id}/videos`
- выбирать только видео:
  - `site = YouTube`
  - `type = Trailer`, `Teaser` или `Clip`
  - желательно `official = true`

YouTube video key сохранять в БД.

Видео не скачивать и не хранить. На frontend показывать через YouTube IFrame API.

## Backend

Создать REST API:

### Movies

`GET /api/movies/popular/sync`

Синхронизирует популярные фильмы из TMDb в PostgreSQL.

### Daily Challenge

`GET /api/challenges/daily`

Возвращает daily challenge:

```json
{
  "challengeId": "uuid",
  "movieId": 123,
  "youtubeKey": "abc123",
  "fragmentStart": 42,
  "stages": [1, 4, 15, 30, 60],
  "currentStage": 1
}
````

Название фильма НЕ отдавать на frontend.

### Submit Answer

`POST /api/challenges/{challengeId}/answer`

Request:

```json
{
  "answer": "Inception",
  "stageSeconds": 4
}
```

Response:

```json
{
  "correct": true,
  "movieTitle": "Inception",
  "score": 750
}
```

Проверять ответ по:

* title
* originalTitle
* lower-case
* trim
* без лишних пробелов
* можно использовать fuzzy matching

### Leaderboard

`GET /api/leaderboard/daily`

Возвращает топ игроков за день.

## Scoring

```text
1 секунда  -> 1000 очков
4 секунды  -> 750 очков
15 секунд -> 500 очков
30 секунд -> 300 очков
60 секунд -> 100 очков
не угадал -> 0
```

## Database

Создать таблицы:

### users

* id uuid primary key
* username varchar
* created_at timestamp

### movies

* id bigint primary key
* tmdb_id bigint unique
* title varchar
* original_title varchar
* release_date date
* poster_path varchar
* created_at timestamp

### movie_videos

* id uuid primary key
* movie_id bigint references movies(id)
* youtube_key varchar
* type varchar
* official boolean
* duration_seconds int nullable
* created_at timestamp

### daily_challenges

* id uuid primary key
* challenge_date date unique
* movie_id bigint references movies(id)
* movie_video_id uuid references movie_videos(id)
* fragment_start int
* created_at timestamp

### attempts

* id uuid primary key
* user_id uuid references users(id)
* challenge_id uuid references daily_challenges(id)
* answer varchar
* stage_seconds int
* correct boolean
* score int
* created_at timestamp

## Redis

Использовать Redis для кеша:

* daily challenge
* leaderboard
* popular movies from TMDb
* TMDb API responses

## Docker Compose

Сделать `docker-compose.yml` с сервисами:

* postgres
* redis
* backend
* frontend

PostgreSQL:

* database: cineguess
* user: cineguess
* password: cineguess

## Frontend UI

Сделать тёмный интерфейс в стиле Songless, но для фильмов.

Layout:

```text
------------------------------------------------
|                  header                      |
------------------------------------------------
|                    |                         |
|  video player      |  game panel             |
|  слева             |  справа                 |
|                    |                         |
------------------------------------------------
```

Левая часть:

* большой прямоугольник с видео
* YouTube iframe
* затемнение/обводка
* видео без показа названия

Правая часть:

* логотип CineGuess
* режим: DAILY MOVIE
* очки
* этапы: 1 / 4 / 15 / 30 / 60
* progress bar
* play / pause button
* volume slider
* input для ответа
* кнопка Submit
* кнопка Skip
* блок результата после ответа

## Цветовая палитра

Использовать тёмную тему, цвета чуть темнее палитры с фиолетовыми медузами.

```css
--color-bg: #0E0B1F;
--color-bg-soft: #151129;
--color-panel: #1D1738;
--color-panel-2: #241B46;

--color-primary: #6F4DB8;
--color-primary-hover: #805ED0;
--color-primary-dark: #4A347F;

--color-accent: #B58AF2;
--color-accent-soft: #8A6BD1;

--color-border: #352A5E;
--color-border-active: #B58AF2;

--color-text: #F2ECFF;
--color-text-muted: #A89BC7;
--color-text-dark: #6E638C;

--color-success: #9DFFB0;
--color-error: #FF6B8A;
```

Фон:

* radial-gradient с тёмным фиолетовым свечением
* карточки полупрозрачные
* border radius 18-24px
* мягкие glow-эффекты на активных элементах

## UI Requirements

Сделать красиво, не дефолтно.

Компоненты:

* `App`
* `DailyChallengePage`
* `VideoPlayer`
* `StageTabs`
* `AnswerInput`
* `VolumeControl`
* `ProgressBar`
* `ScoreBadge`
* `LeaderboardPanel`

## YouTube Player Logic

Frontend должен:

1. Получить challenge с backend.
2. Создать YouTube player.
3. Запускать видео с `fragmentStart`.
4. Останавливать через `stageSeconds`.
5. При следующем этапе увеличивать доступную длину фрагмента.
6. Не показывать название видео.
7. Управлять:

    * play
    * pause
    * volume
    * progress

Пример логики:

```ts
player.loadVideoById({
  videoId: youtubeKey,
  startSeconds: fragmentStart,
  endSeconds: fragmentStart + currentStageSeconds
});
```

## Env

Backend:

```env
TMDB_API_KEY=
SPRING_R2DBC_URL=r2dbc:postgresql://postgres:5432/cineguess
SPRING_R2DBC_USERNAME=cineguess
SPRING_R2DBC_PASSWORD=cineguess
REDIS_HOST=redis
REDIS_PORT=6379
```

Frontend:

```env
VITE_API_BASE_URL=http://localhost:8080
```

## Что нужно сгенерировать

Сгенерируй полностью рабочий проект:

* backend
* frontend
* docker-compose
* миграции Flyway
* README.md
* .env.example
* базовую архитектуру
* API clients
* UI components
* стили
* обработку ошибок
* loading states

## Важные ограничения

* Не скачивать фильмы.
* Не хранить видеофайлы.
* Использовать только YouTube embed через официальные трейлеры/клипы.
* Название фильма не должно попадать на frontend до ответа.
* Daily challenge должен быть одинаковый для всех пользователей в текущий день.
* Код должен быть чистый, структурированный и готовый к расширению.

