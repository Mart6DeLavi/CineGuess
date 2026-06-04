package dev.cineguess.movie;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface MovieRepository extends ReactiveCrudRepository<Movie, Long> {

    Mono<Movie> findByTmdbId(Long tmdbId);
}
