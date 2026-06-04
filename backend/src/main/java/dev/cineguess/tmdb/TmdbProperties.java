package dev.cineguess.tmdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmdb")
public record TmdbProperties(String baseUrl, String apiKey) {
}
