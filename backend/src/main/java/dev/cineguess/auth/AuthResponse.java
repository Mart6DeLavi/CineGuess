package dev.cineguess.auth;

public record AuthResponse(String token, AuthUserResponse user) {
}
