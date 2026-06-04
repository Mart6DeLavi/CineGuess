package dev.cineguess.challenge;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("attempts")
public class Attempt implements Persistable<UUID> {

    @Id
    private UUID id;
    @Transient
    private boolean newEntity;
    private UUID userId;
    private UUID challengeId;
    private String answer;
    private Integer stageSeconds;
    private Boolean correct;
    private Integer score;
    private Instant createdAt;

    public Attempt() {
    }

    public Attempt(UUID id, UUID userId, UUID challengeId, String answer, Integer stageSeconds, Boolean correct, Integer score, Instant createdAt) {
        this.id = id;
        this.newEntity = true;
        this.userId = userId;
        this.challengeId = challengeId;
        this.answer = answer;
        this.stageSeconds = stageSeconds;
        this.correct = correct;
        this.score = score;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getChallengeId() {
        return challengeId;
    }

    public String getAnswer() {
        return answer;
    }

    public Integer getStageSeconds() {
        return stageSeconds;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public Integer getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
