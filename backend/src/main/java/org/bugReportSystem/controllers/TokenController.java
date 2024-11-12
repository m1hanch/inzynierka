package org.bugReportSystem.controllers;

import org.bugReportSystem.exception.ApiError;
import org.bugReportSystem.requests.LoginRequest;
import org.bugReportSystem.services.TokenService;
import org.bugReportSystem.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("api/auth")
public class TokenController {
    private final UserService userService;
    private final TokenService tokenService;

    public TokenController(UserService userService, TokenService tokenService) {
        this.userService = userService;
        this.tokenService = tokenService;
    }

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody LoginRequest requestBody) {
        if ((requestBody.email() == null && requestBody.password() == null) || (Objects.equals(requestBody.email(), "") && Objects.equals(requestBody.password(), ""))) {
            ApiError error = new ApiError("Validation", "Both", "Missing data");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } else if (requestBody.email() == null || requestBody.email().isEmpty()) {
            ApiError error = new ApiError("Validation", "E-mail", "Missing e-mail");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } else if (requestBody.password() == null || requestBody.password().isEmpty()) {
            ApiError error = new ApiError("Validation", "Password", "Missing password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } else {
            return tokenService.loginUser(requestBody.email(), requestBody.password(), userService);
        }
    }

    @GetMapping("refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refreshToken", defaultValue = "") String refreshToken) {
        if (refreshToken == null) {
            ApiError error = new ApiError("Refresh", null, "No token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } else {
            return tokenService.refreshToken(refreshToken);
        }
    }

    @DeleteMapping("logout")
    public ResponseEntity<?> logout(@CookieValue(value = "refreshToken", defaultValue = "") String refreshToken) {
        if (!refreshToken.isEmpty()) {
            try {
                // Usuwanie tokenu
                tokenService.deleteToken(refreshToken);
                // Usuwanie ciasteczka
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                ApiError error = new ApiError("logout", "", "error during logout");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }
        } else {
            // Brak ciasteczka
            ApiError error = new ApiError("logout", "", "missing cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @DeleteMapping("logout-all")
    public ResponseEntity<?> logoutAll(@CookieValue(value = "refreshToken", defaultValue = "") String refreshToken) {
        if (!refreshToken.isEmpty()) {
            try {
                Integer userId = tokenService.getUserIdFromToken(refreshToken);
                // Usunięcie wszystkich tokenów użytkownika z bazy danych
                tokenService.deleteAllTokens(userId);
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
