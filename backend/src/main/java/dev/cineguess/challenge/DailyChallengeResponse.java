package dev.cineguess.challenge;

import java.util.List;
import java.util.UUID;

public record DailyChallengeResponse(
        UUID challengeId,
        Long movieId,
        String youtubeKey,
        Integer fragmentStart,
        List<Integer> stages,
        Integer currentStage
) {
}
