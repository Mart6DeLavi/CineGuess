package dev.cineguess.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String password
) {
}
