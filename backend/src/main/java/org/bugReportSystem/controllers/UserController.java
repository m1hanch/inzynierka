package org.bugReportSystem.controllers;

import org.bugReportSystem.email.MailService;
import org.bugReportSystem.entities.PasswordResetToken;
import org.bugReportSystem.entities.User;
import org.bugReportSystem.exception.ApiError;
import org.bugReportSystem.repositories.TokenResetRepository;
import org.bugReportSystem.repositories.UserRepository;
import org.bugReportSystem.requests.*;
import org.bugReportSystem.services.TokenService;
import org.bugReportSystem.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("api/users")
public class UserController {
    private final UserService userService;
    private final TokenService tokenService;
    private final MailService mailService;
    private final UserRepository userRepository;
    private final TokenResetRepository tokenResetRepository;

    @Autowired
    public UserController(UserService userService, TokenService tokenService, MailService mailService, UserRepository userRepository, TokenResetRepository tokenResetRepository) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.mailService = mailService;
        this.userRepository = userRepository;
        this.tokenResetRepository = tokenResetRepository;
    }

    @GetMapping("{uuid}")
    public ResponseEntity<?> getUser(@PathVariable("uuid") Integer id, HttpServletRequest httpRequest) {
        return userService.getUserDetails(id, userService, httpRequest);
    }

    @GetMapping()
    public ResponseEntity<?> getUserByToken(HttpServletRequest httpRequest) {
        var userId = userService.getUserIDFromAccessToken(httpRequest);
        return userService.getUserDetails(userId, userService, httpRequest);
    }

    @GetMapping("email/{email}")
    public User getUserByEmail(@PathVariable("email") String email) {
        return userService.getUserByEmail(email);
    }

    @PostMapping
    public ResponseEntity<?> addUser(@RequestBody UserRegistrationRequest request) {
        ResponseEntity<?> response = userService.addUser(request);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") Integer id) {
        userService.deleteUser(id);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, value = "{uuid}")
    public ResponseEntity<?> updateUser(
            @PathVariable("uuid") Integer uuid,
            @ModelAttribute UserUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        return userService.updateUser(uuid, request, httpRequest);
    }

    @PostMapping("/reset-pass")
    public ResponseEntity<?> sendPasswordResetEmail(@RequestBody PasswordResetEmail body) {
        try {
            String email = body.email();
            var user = userRepository.findByEmail(email);
            mailService.sendPasswordResetEmail(user.get(), "reset");
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PatchMapping("reset-pass/{token}")
    public ResponseEntity<?> resetPassword(@PathVariable String token, @RequestBody PasswordResetRequest body) {
        if (!body.password().equals(body.retPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Passwords should be the same.");
        }
        String newPassword = body.password();
        newPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        PasswordResetToken reset = tokenResetRepository.findByToken(token);
        if (!userService.hasExpired(reset.getExpiryDateTime())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token.");
        }

        String email = reset.getUser().getEmail();
        var user = userRepository.findByEmail(email);
        if (user.isPresent() && (!Objects.equals(newPassword, user.get().getPassword()))) {
            user.get().setPassword(newPassword);
            userRepository.save(user.get());
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("New password is invalid or matches the old password.");
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activationEmail(@CookieValue(name = "refreshToken", required = false) String refreshToken, HttpServletRequest request) {
        return userService.activationEmail(refreshToken, tokenService, userService, request);
    }

    @PatchMapping("/activate/{token}")
    public ResponseEntity<?> activate(@PathVariable String token) {
        return userService.activateUser(token, tokenService, userService, userRepository);
    }

    @PatchMapping("/pass/{uuid}")
    public ResponseEntity<?> changePassword(@PathVariable Integer uuid, @RequestBody ChangePasswordRequest changePasswordRequest, HttpServletRequest request) {
        ResponseEntity<?> checkAuthorizationResult = userService.checkAuthorization(request);
        if (checkAuthorizationResult.getStatusCode() != HttpStatus.OK) {
            return checkAuthorizationResult;
        }
        Integer userId = userService.getUserIDFromAccessToken(request);
        if (userId != -1) {
            if (userId.equals(uuid)) {
                return userService.changePassword(uuid, changePasswordRequest);
            } else {
                ApiError error = new ApiError("General", null, "You can only modify your own password");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
        } else {
            ApiError error = new ApiError("Access", null, "Getting user id from access token failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}
