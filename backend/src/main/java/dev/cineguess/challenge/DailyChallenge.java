package dev.cineguess.challenge;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Table("daily_challenges")
public class DailyChallenge implements Persistable<UUID> {

    @Id
    private UUID id;
    @Transient
    private boolean newEntity;
    private LocalDate challengeDate;
    private Integer challengeSlot;
    private Long movieId;
    private UUID movieVideoId;
    private Integer fragmentStart;
    private Instant createdAt;

    public DailyChallenge() {
    }

    public DailyChallenge(UUID id, LocalDate challengeDate, Integer challengeSlot, Long movieId, UUID movieVideoId, Integer fragmentStart, Instant createdAt) {
        this.id = id;
        this.newEntity = true;
        this.challengeDate = challengeDate;
        this.challengeSlot = challengeSlot;
        this.movieId = movieId;
        this.movieVideoId = movieVideoId;
        this.fragmentStart = fragmentStart;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public LocalDate getChallengeDate() {
        return challengeDate;
    }

    public Integer getChallengeSlot() {
        return challengeSlot;
    }

    public Long getMovieId() {
        return movieId;
    }

    public UUID getMovieVideoId() {
        return movieVideoId;
    }

    public Integer getFragmentStart() {
        return fragmentStart;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
