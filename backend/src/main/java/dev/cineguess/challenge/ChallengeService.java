package dev.cineguess.challenge;

import dev.cineguess.config.RedisCacheService;
import dev.cineguess.movie.Movie;
import dev.cineguess.movie.MovieRepository;
import dev.cineguess.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ChallengeService {

    private static final int DAILY_CHALLENGE_COUNT = 5;
    private static final String TMDB_POSTER_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final List<Integer> STAGES = List.of(1, 4, 15, 30, 60);
    private static final Map<Integer, Integer> SCORES = Map.of(
            1, 1000,
            4, 750,
            15, 500,
            30, 300,
            60, 100
    );
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9 ]");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final DailyChallengeRepository dailyChallengeRepository;
    private final AttemptRepository attemptRepository;
    private final MovieRepository movieRepository;
    private final UserService userService;
    private final DatabaseClient databaseClient;
    private final RedisCacheService cache;

    public ChallengeService(
            DailyChallengeRepository dailyChallengeRepository,
            AttemptRepository attemptRepository,
            MovieRepository movieRepository,
            UserService userService,
            DatabaseClient databaseClient,
            RedisCacheService cache
    ) {
        this.dailyChallengeRepository = dailyChallengeRepository;
        this.attemptRepository = attemptRepository;
        this.movieRepository = movieRepository;
        this.userService = userService;
        this.databaseClient = databaseClient;
        this.cache = cache;
    }

    public Mono<DailyChallengeResponse> dailyChallenge() {
        return dailyChallenge(0);
    }

    public Mono<DailyChallengeResponse> dailyChallenge(int slot) {
        if (slot < 0 || slot >= DAILY_CHALLENGE_COUNT) {
            return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Daily challenge slot not found"));
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String cacheKey = "daily-challenge:v2:" + today + ":" + slot;
        return cache.getJson(cacheKey, DailyChallengeResponse.class)
                .switchIfEmpty(Mono.defer(() -> dailyChallengeRepository.findByChallengeDateAndChallengeSlot(today, slot)
                        .flatMap(this::toResponse)
                        .switchIfEmpty(createDailyChallenge(today, slot))
                        .flatMap(response -> cache.putJson(cacheKey, response, Duration.ofMinutes(30)).thenReturn(response))));
    }

    public Mono<AnswerResponse> answer(UUID challengeId, AnswerRequest request, String username) {
        return dailyChallengeRepository.findById(challengeId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found")))
                .flatMap(challenge -> movieRepository.findById(challenge.getMovieId())
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found")))
                        .flatMap(movie -> userService.getOrCreate(username)
                                .flatMap(user -> {
                                    boolean correct = isCorrect(request.answer(), movie);
                                    int score = correct ? SCORES.getOrDefault(request.stageSeconds(), 0) : 0;
                                    Attempt attempt = new Attempt(
                                            UUID.randomUUID(),
                                            user.getId(),
                                            challengeId,
                                            request.answer().trim(),
                                            request.stageSeconds(),
                                            correct,
                                            score,
                                            Instant.now()
                                    );
                                    return attemptRepository.save(attempt)
                                            .map(saved -> new AnswerResponse(
                                                    correct,
                                                    movie.getTitle(),
                                                    posterUrl(movie),
                                                    movie.getVoteAverage(),
                                                    score
                                            ));
                                })));
    }

    private Mono<DailyChallengeResponse> createDailyChallenge(LocalDate date, int slot) {
        return randomMovieVideo(date)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No movies with YouTube videos found. Run /api/movies/popular/sync first."
                )))
                .flatMap(selection -> {
                    DailyChallenge challenge = new DailyChallenge(
                            UUID.randomUUID(),
                            date,
                            slot,
                            selection.movieId(),
                            selection.movieVideoId(),
                            (int) (Math.random() * 61),
                            Instant.now()
                    );
                    return dailyChallengeRepository.save(challenge)
                            .map(saved -> new DailyChallengeResponse(
                                    saved.getId(),
                                    selection.movieId(),
                                    selection.youtubeKey(),
                                    saved.getFragmentStart(),
                                    STAGES,
                                    STAGES.getFirst()
                            ));
                });
    }

    private Mono<DailyChallengeResponse> toResponse(DailyChallenge challenge) {
        return databaseClient.sql("""
                        SELECT dc.id challenge_id, dc.movie_id, dc.fragment_start, mv.youtube_key
                        FROM daily_challenges dc
                        JOIN movie_videos mv ON mv.id = dc.movie_video_id
                        WHERE dc.id = :challengeId
                        """)
                .bind("challengeId", challenge.getId())
                .map((row, metadata) -> new DailyChallengeResponse(
                        row.get("challenge_id", UUID.class),
                        row.get("movie_id", Long.class),
                        row.get("youtube_key", String.class),
                        row.get("fragment_start", Integer.class),
                        STAGES,
                        STAGES.getFirst()
                ))
                .one();
    }

    private Mono<MovieVideoSelection> randomMovieVideo(LocalDate date) {
        return databaseClient.sql("""
                        SELECT m.id movie_id, mv.id movie_video_id, mv.youtube_key
                        FROM movies m
                        JOIN movie_videos mv ON mv.movie_id = m.id
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM daily_challenges dc
                            WHERE dc.challenge_date = :challengeDate
                              AND dc.movie_id = m.id
                        )
                        ORDER BY random()
                        LIMIT 1
                        """)
                .bind("challengeDate", date)
                .map((row, metadata) -> new MovieVideoSelection(
                        row.get("movie_id", Long.class),
                        row.get("movie_video_id", UUID.class),
                        row.get("youtube_key", String.class)
                ))
                .one();
    }

    private boolean isCorrect(String answer, Movie movie) {
        String normalizedAnswer = normalize(answer);
        String title = normalize(movie.getTitle());
        String originalTitle = normalize(movie.getOriginalTitle());
        return title.equals(normalizedAnswer)
                || originalTitle.equals(normalizedAnswer)
                || fuzzyMatch(normalizedAnswer, title)
                || fuzzyMatch(normalizedAnswer, originalTitle);
    }

    private boolean fuzzyMatch(String answer, String title) {
        if (answer.isBlank() || title.isBlank()) {
            return false;
        }
        int maxDistance = title.length() <= 8 ? 1 : 2;
        return levenshtein(answer, title) <= maxDistance;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
        String lower = withoutDiacritics.toLowerCase(Locale.ROOT);
        String alnum = NON_ALNUM.matcher(lower).replaceAll(" ");
        return SPACES.matcher(alnum).replaceAll(" ").trim();
    }

    private String posterUrl(Movie movie) {
        if (movie.getPosterPath() == null || movie.getPosterPath().isBlank()) {
            return null;
        }
        return TMDB_POSTER_BASE_URL + movie.getPosterPath();
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] temp = previous;
            previous = current;
            current = temp;
        }
        return previous[right.length()];
    }

    private record MovieVideoSelection(Long movieId, UUID movieVideoId, String youtubeKey) {
    }
}
