package dev.cineguess.tmdb;

public record TmdbVideo(
        String key,
        String site,
        String type,
        Boolean official
) {
}
