package dev.cineguess.leaderboard;

import com.fasterxml.jackson.core.type.TypeReference;
import dev.cineguess.config.RedisCacheService;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class LeaderboardService {

    private final DatabaseClient databaseClient;
    private final RedisCacheService cache;

    public LeaderboardService(DatabaseClient databaseClient, RedisCacheService cache) {
        this.databaseClient = databaseClient;
        this.cache = cache;
    }

    public Mono<List<LeaderboardEntry>> dailyLeaderboard() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String cacheKey = "leaderboard:daily:" + today;
        TypeReference<List<LeaderboardEntry>> type = new TypeReference<>() {
        };
        return cache.getJson(cacheKey, type)
                .switchIfEmpty(Mono.defer(() -> queryLeaderboard(today)
                        .flatMap(entries -> cache.putJson(cacheKey, entries, Duration.ofSeconds(15)).thenReturn(entries))));
    }

    private Mono<List<LeaderboardEntry>> queryLeaderboard(LocalDate date) {
        return databaseClient.sql("""
                        SELECT u.username, max(a.score)::int score, count(a.id)::int attempts
                        FROM attempts a
                        JOIN users u ON u.id = a.user_id
                        JOIN daily_challenges dc ON dc.id = a.challenge_id
                        WHERE dc.challenge_date = :challengeDate
                        GROUP BY u.id, u.username
                        ORDER BY score DESC, attempts ASC, u.username ASC
                        LIMIT 10
                        """)
                .bind("challengeDate", date)
                .map((row, metadata) -> new LeaderboardEntry(
                        row.get("username", String.class),
                        row.get("score", Integer.class),
                        row.get("attempts", Integer.class)
                ))
                .all()
                .collectList();
    }
}
