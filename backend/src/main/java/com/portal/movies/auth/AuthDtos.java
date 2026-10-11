package com.portal.movies.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(@NotBlank @Size(max=100) String name,
                                  @NotBlank @Email @Size(max=190) String email,
                                  @NotBlank @Size(min=8, max=72, message="Password must be 8 to 72 characters") String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UserView(Long id, String name, String email) {}
    public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserView user) {}
}
