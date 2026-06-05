package dev.cineguess.auth;

import dev.cineguess.user.User;
import dev.cineguess.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public Mono<AuthResponse> register(AuthRequest request) {
        String email = normalizeEmail(request.email());
        return userRepository.findByEmail(email)
                .flatMap(existing -> Mono.<User>error(new ResponseStatusException(CONFLICT, "Email is already registered")))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = new User(
                            UUID.randomUUID(),
                            email,
                            email,
                            passwordEncoder.encode(request.password()),
                            Instant.now()
                    );
                    return userRepository.save(user);
                }))
                .map(this::toResponse);
    }

    public Mono<AuthResponse> login(AuthRequest request) {
        String email = normalizeEmail(request.email());
        return userRepository.findByEmail(email)
                .filter(user -> user.getPasswordHash() != null && passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .switchIfEmpty(Mono.error(new ResponseStatusException(UNAUTHORIZED, "Invalid email or password")))
                .map(this::toResponse);
    }

    private AuthResponse toResponse(User user) {
        AuthUserResponse authUser = new AuthUserResponse(user.getId(), user.getEmail());
        return new AuthResponse(jwtService.createToken(user), authUser);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
