package dev.cineguess.challenge;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

public interface DailyChallengeRepository extends ReactiveCrudRepository<DailyChallenge, UUID> {

    Mono<DailyChallenge> findByChallengeDate(LocalDate challengeDate);

    Mono<DailyChallenge> findByChallengeDateAndChallengeSlot(LocalDate challengeDate, Integer challengeSlot);
}
