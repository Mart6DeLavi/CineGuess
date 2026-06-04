package dev.cineguess.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class RedisCacheService {

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisCacheService(ReactiveStringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public Mono<String> get(String key) {
        return redis.opsForValue().get(key);
    }

    public Mono<Boolean> put(String key, String value, Duration ttl) {
        return redis.opsForValue().set(key, value, ttl).onErrorReturn(false);
    }

    public <T> Mono<T> getJson(String key, Class<T> type) {
        return get(key).flatMap(value -> {
            try {
                return Mono.just(objectMapper.readValue(value, type));
            } catch (JsonProcessingException ex) {
                return Mono.empty();
            }
        }).onErrorResume(ex -> Mono.empty());
    }

    public <T> Mono<T> getJson(String key, TypeReference<T> type) {
        return get(key).flatMap(value -> {
            try {
                return Mono.just(objectMapper.readValue(value, type));
            } catch (JsonProcessingException ex) {
                return Mono.empty();
            }
        }).onErrorResume(ex -> Mono.empty());
    }

    public Mono<Boolean> putJson(String key, Object value, Duration ttl) {
        try {
            return put(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException ex) {
            return Mono.just(false);
        }
    }
}
