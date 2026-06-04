package dev.cineguess.movie;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieSyncService movieSyncService;

    public MovieController(MovieSyncService movieSyncService) {
        this.movieSyncService = movieSyncService;
    }

    @GetMapping("/popular/sync")
    public Mono<MovieSyncResponse> syncPopular() {
        return movieSyncService.syncPopular();
    }
}
