package dev.cineguess.user;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> getOrCreate(String username) {
        String normalized = StringUtils.hasText(username) ? username.trim() : "guest";
        return userRepository.findByUsername(normalized)
                .switchIfEmpty(userRepository.save(new User(UUID.randomUUID(), normalized, Instant.now())));
    }
}
