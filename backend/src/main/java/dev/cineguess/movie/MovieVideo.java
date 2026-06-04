package dev.cineguess.movie;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("movie_videos")
public class MovieVideo implements Persistable<UUID> {

    @Id
    private UUID id;
    @Transient
    private boolean newEntity;
    private Long movieId;
    private String youtubeKey;
    private String type;
    private Boolean official;
    private Integer durationSeconds;
    private Instant createdAt;

    public MovieVideo() {
    }

    public MovieVideo(UUID id, Long movieId, String youtubeKey, String type, Boolean official, Integer durationSeconds, Instant createdAt) {
        this.id = id;
        this.newEntity = true;
        this.movieId = movieId;
        this.youtubeKey = youtubeKey;
        this.type = type;
        this.official = official;
        this.durationSeconds = durationSeconds;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public Long getMovieId() {
        return movieId;
    }

    public String getYoutubeKey() {
        return youtubeKey;
    }

    public String getType() {
        return type;
    }

    public Boolean getOfficial() {
        return official;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
