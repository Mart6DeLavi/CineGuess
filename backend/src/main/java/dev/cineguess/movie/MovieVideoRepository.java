package dev.cineguess.movie;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface MovieVideoRepository extends ReactiveCrudRepository<MovieVideo, UUID> {

    Mono<MovieVideo> findByMovieIdAndYoutubeKey(Long movieId, String youtubeKey);
}
