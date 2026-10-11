package com.portal.movies.auth;

import com.portal.movies.domain.AppUser;
import com.portal.movies.repository.AppUserRepository;
import com.portal.movies.web.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long expirationMinutes;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       @Value("${app.jwt.expiration-minutes:120}") long expirationMinutes) {
        this.users=users; this.passwordEncoder=passwordEncoder; this.jwtService=jwtService; this.expirationMinutes=expirationMinutes;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists.");
        AppUser user = users.save(new AppUser(request.name().trim(), email, passwordEncoder.encode(request.password())));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> ApiException.unauthorized("Email or password is incorrect."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))
            throw ApiException.unauthorized("Email or password is incorrect.");
        return response(user);
    }

    private AuthDtos.AuthResponse response(AppUser user) {
        return new AuthDtos.AuthResponse(jwtService.issue(user.getId(), user.getEmail()), "Bearer", expirationMinutes*60,
                new AuthDtos.UserView(user.getId(), user.getDisplayName(), user.getEmail()));
    }
}
