package dev.cineguess.tmdb;

import java.util.List;

public record TmdbPopularResponse(List<TmdbMovieSummary> results) {
}
