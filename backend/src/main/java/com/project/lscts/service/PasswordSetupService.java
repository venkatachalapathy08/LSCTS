
package com.project.lscts.service;

import com.project.lscts.dto.user.SetupPasswordRequestDTO;
import com.project.lscts.dto.user.AdminCreateUserRequestDTO;
import com.project.lscts.entity.PasswordSetupTokenEntity;
import com.project.lscts.entity.Role;
import com.project.lscts.entity.UserEntity;
import com.project.lscts.repository.PasswordSetupTokenRepository;
import com.project.lscts.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordSetupService {

    private final UserRepository userRepository;
    private final PasswordSetupTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordSetupService(
            UserRepository userRepository,
            PasswordSetupTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // Admin creates a resident account
    @Transactional
    public String createResident(AdminCreateUserRequestDTO request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
        }

        UserEntity user = new UserEntity();

        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(Role.USER);

        // Temporary random password; resident sets their own password.
        byte[] temporaryBytes = new byte[32];
        secureRandom.nextBytes(temporaryBytes);

        String temporaryPassword = Base64.getEncoder()
                .encodeToString(temporaryBytes);

        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setPasswordSetupCompleted(false);

        UserEntity savedUser = userRepository.save(user);

        // Generate a secure setup token.
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        PasswordSetupTokenEntity setupToken =
                new PasswordSetupTokenEntity();

        setupToken.setUser(savedUser);
        setupToken.setTokenHash(sha256(rawToken));
        setupToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        setupToken.setUsed(false);

        tokenRepository.save(setupToken);

        String setupLink =
                "http://localhost:5173/setup-password?token=" + rawToken;

        emailService.sendPasswordSetupEmail(
                savedUser.getEmail(),
                setupLink
        );

        return "Resident account created. Password setup link sent to email.";
    }

    // Resident sets their initial password

    @Transactional
    public UserEntity setupPassword(SetupPasswordRequestDTO request) {

        String tokenHash = sha256(request.getSetupToken());

        PasswordSetupTokenEntity setupToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid or expired password setup link"
                        ));

        if (setupToken.isUsed()
                || !setupToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired password setup link"
            );
        }

        UserEntity user = setupToken.getUser();

        if (user.isPasswordSetupCompleted()
                || user.getRole() != Role.USER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired password setup link"
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        user.setPasswordSetupCompleted(true);

        UserEntity savedUser = userRepository.save(user);

        setupToken.setUsed(true);
        tokenRepository.save(setupToken);

        return savedUser;
    }


    // Cancel a resident account before initial password setup
    @Transactional
    public String cancelPendingResident(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resident not found"
                ));

        if (user.isPasswordSetupCompleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resident has already completed password setup"
            );
        }

        tokenRepository.deleteByUserUserId(userId);
        userRepository.delete(user);

        return "Pending resident account cancelled successfully.";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to process password setup token",
                    ex
            );
        }
    }
}
