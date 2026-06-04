package dev.cineguess.tmdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.cineguess.config.RedisCacheService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class TmdbClient {

    private static final Duration CACHE_TTL = Duration.ofHours(12);

    private final WebClient webClient;
    private final TmdbProperties properties;
    private final RedisCacheService cache;
    private final ObjectMapper objectMapper;

    public TmdbClient(WebClient.Builder webClientBuilder, TmdbProperties properties, RedisCacheService cache, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.cache = cache;
        this.objectMapper = objectMapper;
    }

    public Mono<TmdbPopularResponse> popularMovies() {
        return cachedGet("tmdb:popular:movies", "/movie/popular", TmdbPopularResponse.class);
    }

    public Mono<TmdbMovieSummary> movie(Long tmdbId) {
        return cachedGet("tmdb:movie:" + tmdbId, "/movie/" + tmdbId, TmdbMovieSummary.class);
    }

    public Mono<TmdbVideosResponse> videos(Long tmdbId) {
        return cachedGet("tmdb:movie:" + tmdbId + ":videos", "/movie/" + tmdbId + "/videos", TmdbVideosResponse.class);
    }

    private <T> Mono<T> cachedGet(String cacheKey, String uri, Class<T> type) {
        return cache.getJson(cacheKey, type)
                .switchIfEmpty(Mono.defer(() -> fetch(uri)
                        .flatMap(body -> cache.put(cacheKey, body, CACHE_TTL).thenReturn(body))
                        .flatMap(body -> {
                            try {
                                return Mono.just(objectMapper.readValue(body, type));
                            } catch (Exception ex) {
                                return Mono.error(ex);
                            }
                        })));
    }

    private Mono<String> fetch(String uri) {
        if (!StringUtils.hasText(properties.apiKey())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "TMDB_API_KEY is required"));
        }
        return webClient.get()
                .uri(uriBuilder -> tmdbUri(uriBuilder, uri))
                .headers(headers -> {
                    if (usesBearerToken()) {
                        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey());
                    }
                })
                .retrieve()
                .bodyToMono(String.class);
    }

    private java.net.URI tmdbUri(UriBuilder uriBuilder, String uri) {
        UriBuilder builder = uriBuilder.path(uri).queryParam("language", "en-US");
        if (!usesBearerToken()) {
            builder.queryParam("api_key", properties.apiKey());
        }
        return builder.build();
    }

    private boolean usesBearerToken() {
        return properties.apiKey().startsWith("eyJ");
    }
}
