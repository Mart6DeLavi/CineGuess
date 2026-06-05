package dev.cineguess.challenge;

import dev.cineguess.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping("/daily")
    public Mono<DailyChallengeResponse> dailyChallenge() {
        return challengeService.dailyChallenge();
    }

    @GetMapping("/daily/{slot}")
    public Mono<DailyChallengeResponse> dailyChallenge(@PathVariable int slot) {
        return challengeService.dailyChallenge(slot);
    }

    @PostMapping("/{challengeId}/answer")
    public Mono<AnswerResponse> answer(
            @PathVariable UUID challengeId,
            @Valid @RequestBody AnswerRequest request,
            Authentication authentication
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return challengeService.answer(challengeId, request, user.id());
    }
}
