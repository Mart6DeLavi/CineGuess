package dev.cineguess.user;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> getOrCreate(String username) {
        String normalized = StringUtils.hasText(username) ? username.trim() : "guest";
        return userRepository.findByUsername(normalized)
                .switchIfEmpty(userRepository.save(new User(UUID.randomUUID(), normalized, normalized, null, Instant.now())));
    }

    public Mono<User> findById(UUID userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(NOT_FOUND, "User not found")));
    }
}
