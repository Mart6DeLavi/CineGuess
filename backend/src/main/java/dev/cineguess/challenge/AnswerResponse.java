package dev.cineguess.challenge;

public record AnswerResponse(
        boolean correct,
        String movieTitle,
        String posterUrl,
        Double tmdbRating,
        int score
) {
}
