package dev.cineguess.challenge;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface AttemptRepository extends ReactiveCrudRepository<Attempt, UUID> {
}
