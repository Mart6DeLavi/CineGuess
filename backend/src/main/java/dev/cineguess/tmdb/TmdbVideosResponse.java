package dev.cineguess.tmdb;

import java.util.List;

public record TmdbVideosResponse(Long id, List<TmdbVideo> results) {
}
