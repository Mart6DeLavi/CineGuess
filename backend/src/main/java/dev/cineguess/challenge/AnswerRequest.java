package dev.cineguess.challenge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
        @NotBlank String answer,
        @NotNull Integer stageSeconds
) {
}
