package dev.cineguess.movie;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;

@Table("movies")
public class Movie {

    @Id
    private Long id;
    private Long tmdbId;
    private String title;
    private String originalTitle;
    private LocalDate releaseDate;
    private String posterPath;
    private Double voteAverage;
    private Instant createdAt;

    public Movie() {
    }

    public Movie(Long id, Long tmdbId, String title, String originalTitle, LocalDate releaseDate, String posterPath, Double voteAverage, Instant createdAt) {
        this.id = id;
        this.tmdbId = tmdbId;
        this.title = title;
        this.originalTitle = originalTitle;
        this.releaseDate = releaseDate;
        this.posterPath = posterPath;
        this.voteAverage = voteAverage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTmdbId() {
        return tmdbId;
    }

    public String getTitle() {
        return title;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public String getPosterPath() {
        return posterPath;
    }

    public Double getVoteAverage() {
        return voteAverage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
