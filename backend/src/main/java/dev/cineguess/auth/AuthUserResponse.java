package dev.cineguess.auth;

import java.util.UUID;

public record AuthUserResponse(UUID id, String email) {
}
