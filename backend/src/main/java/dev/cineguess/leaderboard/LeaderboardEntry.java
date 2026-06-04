package dev.cineguess.leaderboard;

public record LeaderboardEntry(
        String username,
        Integer score,
        Integer attempts
) {
}
