package dev.cineguess.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TmdbMovieSummary(
        Long id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("release_date") LocalDate releaseDate,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("vote_average") Double voteAverage
) {
}
