package dev.cineguess.movie;

import dev.cineguess.tmdb.TmdbClient;
import dev.cineguess.tmdb.TmdbMovieSummary;
import dev.cineguess.tmdb.TmdbVideo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class MovieSyncService {

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("trailer", "teaser", "clip");

    private final TmdbClient tmdbClient;
    private final MovieRepository movieRepository;
    private final MovieVideoRepository movieVideoRepository;

    public MovieSyncService(TmdbClient tmdbClient, MovieRepository movieRepository, MovieVideoRepository movieVideoRepository) {
        this.tmdbClient = tmdbClient;
        this.movieRepository = movieRepository;
        this.movieVideoRepository = movieVideoRepository;
    }

    public Mono<MovieSyncResponse> syncPopular() {
        return tmdbClient.popularMovies()
                .flatMapMany(response -> Flux.fromIterable(response.results()))
                .flatMap(this::syncMovie, 4)
                .reduce(new MovieSyncResponse(0, 0), (left, right) ->
                        new MovieSyncResponse(left.moviesSynced() + right.moviesSynced(), left.videosSynced() + right.videosSynced()));
    }

    private Mono<MovieSyncResponse> syncMovie(TmdbMovieSummary summary) {
        return movieRepository.findByTmdbId(summary.id())
                .map(movie -> new Movie(
                        movie.getId(),
                        movie.getTmdbId(),
                        summary.title(),
                        summary.originalTitle(),
                        summary.releaseDate(),
                        summary.posterPath(),
                        summary.voteAverage(),
                        movie.getCreatedAt()
                ))
                .switchIfEmpty(Mono.just(new Movie(
                        null,
                        summary.id(),
                        summary.title(),
                        summary.originalTitle(),
                        summary.releaseDate(),
                        summary.posterPath(),
                        summary.voteAverage(),
                        Instant.now()
                )))
                .flatMap(movieRepository::save)
                .flatMap(movie -> tmdbClient.videos(summary.id())
                        .flatMapMany(response -> Flux.fromIterable(response.results()))
                        .filter(this::isPlayableMovieVideo)
                        .flatMap(video -> saveVideoIfMissing(movie, video), 3)
                        .count()
                        .map(count -> new MovieSyncResponse(1, count.intValue())));
    }

    private boolean isPlayableMovieVideo(TmdbVideo video) {
        return "YouTube".equalsIgnoreCase(video.site())
                && video.key() != null
                && ALLOWED_VIDEO_TYPES.contains(video.type().toLowerCase(Locale.ROOT));
    }

    private Mono<MovieVideo> saveVideoIfMissing(Movie movie, TmdbVideo video) {
        return movieVideoRepository.findByMovieIdAndYoutubeKey(movie.getId(), video.key())
                .switchIfEmpty(movieVideoRepository.save(new MovieVideo(
                        UUID.randomUUID(),
                        movie.getId(),
                        video.key(),
                        video.type(),
                        Boolean.TRUE.equals(video.official()),
                        null,
                        Instant.now()
                )));
    }
}
