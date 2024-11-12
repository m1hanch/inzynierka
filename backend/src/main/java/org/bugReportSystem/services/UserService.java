package org.bugReportSystem.services;

import org.bugReportSystem.daos.UserDAO;
import org.bugReportSystem.dtos.UserDTO;
import org.bugReportSystem.email.MailService;
import org.bugReportSystem.entities.User;
import org.bugReportSystem.exception.ApiError;
import org.bugReportSystem.exception.ResourceNotFoundException;
import org.bugReportSystem.repositories.TokenResetRepository;
import org.bugReportSystem.repositories.UserRepository;
import org.bugReportSystem.requests.ChangePasswordRequest;
import org.bugReportSystem.requests.UserRegistrationRequest;
import org.bugReportSystem.requests.UserUpdateRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.regions.Region;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.bugReportSystem.services.TokenService.EXPIRATION_TIME_REFRESH;
import static org.bugReportSystem.services.TokenService.generateToken;

@Service
public class UserService {
    private final UserDAO userDAO;

    @Autowired
    JavaMailSender javaMailSender;

    @Autowired
    TokenResetRepository tokenResetRepository;

    @Autowired
    MailService mailService;
    @Autowired
    TokenService tokenService;

    @Value("${aws.s3.access-key-id}")
    private String S3accessKeyId;

    @Value("${aws.s3.secret-access-key}")
    private String S3SecretAccessKey;

    @Value("${aws.s3.bucket}")
    private String S3BucketName;

    @Value("${aws.s3.region}")
    private Region S3Region;

    @Value("${S3profilePicsFolder}")
    private String S3ProfilePicsFolder;

    @Autowired
    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    public User getUserById(Integer id) {
        return userDAO.getUserById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer with id [%s] not found".formatted(id))
                );
    }

    public User getUserByEmail(String email) {
        return userDAO.getUserByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer with email [%s] not found".formatted(email))
                );
    }

    private ResponseEntity<?> checkEmailExists(String email) {
        if (userDAO.existsUserWithEmail(email)) {
            ApiError error = new ApiError("Validation", "email", "Email already exists");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        return ResponseEntity.ok("Email checked.");
    }

    public ResponseEntity<?> passwordValidator(String password, String retPassword) {
        if (password.length() < 8 || password.length() > 32) {
            ApiError error = new ApiError("Validation", "password", "Password length should be between 8 and 32 characters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            ApiError error = new ApiError("Validation", "password", "Password should contain at least one special character");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (!password.matches(".*\\d.*")) {
            ApiError error = new ApiError("Validation", "password", "Password should contain at least one digit");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (password.contains(" ")) {
            ApiError error = new ApiError("Validation", "password", "Password should not contain spaces");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (!password.equals(retPassword)) {
            ApiError error = new ApiError("Validation", "retPassword", "Invalid retPassword");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        return ResponseEntity.ok("password is approved.");
    }

    private ResponseEntity<?> checkFullName(String firstname, String lastname) {
        if (firstname.length() > 50) {
            ApiError error = new ApiError("Validation", "firstname", "Invalid firstname");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        if (lastname.length() > 50) {
            ApiError error = new ApiError("Validation", "lastname", "Invalid lastname");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        return ResponseEntity.ok("fullname checked.");
    }

    public ResponseEntity<?> addUser(UserRegistrationRequest userRegistrationRequest) {
        final String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
        if (isNullOrEmpty(userRegistrationRequest.firstname()) ||
                isNullOrEmpty(userRegistrationRequest.lastname()) ||
                isNullOrEmpty(userRegistrationRequest.email()) ||
                isNullOrEmpty(userRegistrationRequest.password())) {
            ApiError error = new ApiError("Validation", null, "Missing data");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        String firstname = userRegistrationRequest.firstname();
        String lastname = userRegistrationRequest.lastname();
        String email = userRegistrationRequest.email();
        String password = userRegistrationRequest.password();

        if (!checkEmailValid(email, regexPattern) || email.length() > 255) {
            ApiError error = new ApiError("Validation", "email", "Invalid email");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        ResponseEntity<?> checkFullNameResult = checkFullName(firstname, lastname);
        if (checkFullNameResult.getStatusCode() != HttpStatus.OK) {
            return checkFullNameResult;
        }
        ResponseEntity<?> checkEmailExistsResult = checkEmailExists(email);
        if (checkEmailExistsResult.getStatusCode() != HttpStatus.OK) {
            return checkEmailExistsResult;
        }


        // dodac dla pozostalych pól sprawdzenia
        // szczegolnie sprawdzic czy nie ma nikt linka takiego jak podany, jak tak to trzeba ladny error wyswietlic na froncie zeby link zostal zmieniony, bez przkierowania ani wywalania, trzeba na froncie to obsluzyc

        //Encrypting password
        String generatedSecuredPasswordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        User user = new User();
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setEmail(email);
        user.setPassword(generatedSecuredPasswordHash);
        userDAO.addUser(user);


        // logujemy od razu poki co po rejestracji, bez aktywacji
        String accessToken = generateToken(EXPIRATION_TIME_REFRESH, user.getId());

        userDAO.addUser(user);
        return ResponseEntity.ok(accessToken);
    }

    public boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public void deleteUser(Integer id) {
        User user = userDAO.getUserById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer with id [%s] not found".formatted(id))
                );

        userDAO.deleteUser(user);
    }

    private boolean checkEmailValid(String email, String emailRegex) {
        return Pattern.compile(emailRegex)
                .matcher(email)
                .matches();
    }

    public ResponseEntity<?> updateUser(Integer uuid, UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        ResponseEntity<?> checkAuthorizationResult = checkAuthorization(request);
        if (checkAuthorizationResult.getStatusCode() != HttpStatus.OK) {
            return checkAuthorizationResult;
        }
        User user = userDAO.getUserById(uuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer with id [%s] not found".formatted(uuid))
                );

        if (userUpdateRequest.password() != null) {
            var generatedSecuredPasswordHash = BCrypt.hashpw(userUpdateRequest.password(), BCrypt.gensalt(12));
            updateIfNotNull(user::setPassword, generatedSecuredPasswordHash);
        }

        updateIfNotNull(user::setFirstname, userUpdateRequest.firstname());
        updateIfNotNull(user::setLastname, userUpdateRequest.lastname());
        updateIfNotNull(user::setEmail, userUpdateRequest.email());
        MultipartFile profilePicture = userUpdateRequest.profilePicture();

//        if (profilePicture != null && !profilePicture.isEmpty()) {
//            String key = S3ProfilePicsFolder + uuid + "/" + profilePicture.getOriginalFilename();
//            handleProfilePicture(profilePicture, user, key);
//            user.setProfilePicture("https://" + S3BucketName + ".s3.amazonaws.com/" + key);
//        }
        userDAO.updateUser(user);
        return ResponseEntity.ok("User updated successfully");
    }

//    private void handleProfilePicture(MultipartFile profilePicture, User user, String key) {
//        try (S3Client s3 = S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(S3accessKeyId, S3SecretAccessKey))).region(S3Region).build()) {
//            // Deleting the old image
//            if(user.getProfilePicture() != null)
//            {
//                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
//                        .bucket(S3BucketName)
//                        .key(user.getProfilePicture().substring(user.getProfilePicture().indexOf(S3ProfilePicsFolder)))
//                        .build();
//                s3.deleteObject(deleteRequest);
//            }
//
//
//            // Uploading the new image
//            PutObjectRequest putRequest = PutObjectRequest.builder()
//                    .bucket(S3BucketName)
//                    .key(key)
//                    .build();
//            s3.putObject(putRequest, RequestBody.fromInputStream(profilePicture.getInputStream(), profilePicture.getSize()));
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to upload profile picture to S3.", e);
//        }
//    }

    private <T> void updateIfNotNull(Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }


    public boolean hasExpired(LocalDateTime expiryDateTime) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        return expiryDateTime.isAfter(currentDateTime);
    }


    public ResponseEntity<?> activationEmail(String refreshToken, TokenService tokenService, UserService userDetailsService, HttpServletRequest request) {
        ResponseEntity<?> checkAuthorizationResult = checkAuthorization(request);
        if (checkAuthorizationResult.getStatusCode() != HttpStatus.OK) {
            return checkAuthorizationResult;
        }
        // Token is valid, proceed to activate and send email
        // DO POPRAWY - ALBO DODAC NOWA FUNKJCE SENDACTIVATIONMAIL ALBO DOSTOSOWAC OBECNA
        User user = userDetailsService.getUserById(tokenService.getUserIdFromToken(refreshToken));
        mailService.sendPasswordResetEmail(user, "activate");
        return ResponseEntity.ok("Activation successful.");
    }

    public ResponseEntity<?> activateUser(String token, TokenService tokenService, UserService userDetailsService, UserRepository userRepository) {
        if (tokenService.isTokenExpired(token))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token expired");
        try {
            User user = userDetailsService.getUserById(tokenService.getUserIdFromToken(token));
            if (user.isActive())
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User already activated");
            user.setActive(true);
            userRepository.save(user);
            return ResponseEntity.ok("Account activated successfully.");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Invalid token: User not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }


    public ResponseEntity<?> changePassword(Integer userId, ChangePasswordRequest changePasswordRequest) {
        User user = getUserById(userId);
        String currentPassword = changePasswordRequest.currentPassword();
        String newPassword = changePasswordRequest.newPassword();
        String retNewPassword = changePasswordRequest.retNewPassword();
        if (!BCrypt.checkpw(currentPassword, user.getPassword())) {
            ApiError error = new ApiError("Validation", "Password", "Invalid password");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        ResponseEntity<?> passwordValidationResult = passwordValidator(newPassword, retNewPassword);
        if (passwordValidationResult.getStatusCode() != HttpStatus.OK) {
            return passwordValidationResult;
        }
        String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        user.setPassword(hashedNewPassword);
        userDAO.updateUser(user);
        tokenService.deleteAllTokens(userId);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> checkAuthorization(HttpServletRequest request) {
        if (checkAuthorizationHeader(request)) {
            try {
                if (checkLoggedUser(request)) {
                    return ResponseEntity.ok("Account logged in successfully.");
                } else {
                    ApiError error = new ApiError("Access", null, "Login failed");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
                }
            } catch (Exception e) {
                ApiError error = new ApiError("Access", null, "Invalid token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        } else {
            ApiError error = new ApiError("General", null, "Missing authorization");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    boolean checkAuthorizationHeader(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        return authorizationHeader != null && authorizationHeader.startsWith("Bearer ");
    }

    boolean checkLoggedUser(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwtToken = authorizationHeader.substring(7);
            return tokenService.validateAccessToken(jwtToken);
        }
        return false;
    }

    public int getUserIDFromAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String jwtToken = authorizationHeader.substring(7);
            if (tokenService.validateAccessToken(jwtToken)) {
                return tokenService.getUserIdFromToken(jwtToken);
            }
        }
        return -1;
    }

    public ResponseEntity<?> getUserDetails(Integer uuid, UserService userService, HttpServletRequest request) {
        //Check if user is logged in
        ResponseEntity<?> checkAuthorizationResult = checkAuthorization(request);
        if (checkAuthorizationResult.getStatusCode() != HttpStatus.OK) {
            return checkAuthorizationResult;
        }
        var user = userService.getUserById(uuid);
        var userDTO = new UserDTO(user.getId(), user.getFirstname(), user.getLastname(),
                user.getEmail(), user.getPassword());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userDTO);
    }
}
