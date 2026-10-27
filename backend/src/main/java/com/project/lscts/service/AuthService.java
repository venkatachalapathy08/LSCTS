package com.project.lscts.service;

import com.project.lscts.dto.auth.ForgotPasswordRequestDTO;
import com.project.lscts.dto.auth.LoginRequestDTO;
import com.project.lscts.dto.auth.LoginResponseDTO;
import com.project.lscts.dto.auth.ResetPasswordRequestDTO;
import com.project.lscts.dto.auth.VerifyOtpRequestDTO;
import com.project.lscts.entity.PasswordResetOtpEntity;
import com.project.lscts.entity.PasswordResetTokenEntity;
import com.project.lscts.entity.UserEntity;
import com.project.lscts.repository.PasswordResetOtpRepository;
import com.project.lscts.repository.PasswordResetTokenRepository;
import com.project.lscts.repository.UserRepository;
import com.project.lscts.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
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
public class AuthService {

    private static final String GENERIC_RESET_MESSAGE =
            "If an account exists for this email, a password reset OTP will be sent.";

    private static final int MAX_OTP_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpAttemptService otpAttemptService;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordResetOtpRepository otpRepository,
            PasswordResetTokenRepository resetTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            OtpAttemptService otpAttemptService) {

        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.otpRepository = otpRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.otpAttemptService = otpAttemptService;
    }

    // Login
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {

        String email = normalizeEmail(request.getEmail());

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        // Initial password setup must be completed before login.
        if (!user.isPasswordSetupCompleted()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return new LoginResponseDTO(
                user.getUserId(),
                user.getName(),
                token,
                user.getRole()
        );
    }

    // Step 1: Generate and email OTP
    @Transactional
    public String forgotPassword(ForgotPasswordRequestDTO request) {

        String email = normalizeEmail(request.getEmail());

        UserEntity user = userRepository.findByEmail(email)
                .orElse(null);

        // Do not reveal account existence or setup status.
        if (user == null || !user.isPasswordSetupCompleted()) {
            return GENERIC_RESET_MESSAGE;
        }

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        PasswordResetOtpEntity otpEntity =
                new PasswordResetOtpEntity();

        otpEntity.setUser(user);
        otpEntity.setOtpHash(passwordEncoder.encode(otp));
        otpEntity.setExpiresAt(
                LocalDateTime.now().plusMinutes(5)
        );
        otpEntity.setUsed(false);
        otpEntity.setFailedAttempts(0);

        // Invalidate previous OTPs and reset tokens.
        otpRepository.deleteByUserUserId(user.getUserId());
        resetTokenRepository.deleteByUserUserId(user.getUserId());

        otpRepository.save(otpEntity);

        emailService.sendPasswordResetOtpEmail(email, otp);

        return GENERIC_RESET_MESSAGE;
    }

    // Step 2: Verify OTP and issue reset token
    // Step 3: Validate reset token and update password

    @Transactional
    public String resetPassword(ResetPasswordRequestDTO request) {

        String tokenHash = sha256(request.getResetToken());

        PasswordResetTokenEntity resetToken =
                resetTokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid or expired reset token"
                        ));

        if (resetToken.isUsed()
                || !resetToken.getExpiresAt().isAfter(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired reset token"
            );
        }

        UserEntity user = resetToken.getUser();

        if (!user.isPasswordSetupCompleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired reset token"
            );
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);

        // Delete reset tokens after successful password change.
        resetTokenRepository.deleteByUserUserId(user.getUserId());

        return "Password reset successfully. Please log in again.";
    }
    @Transactional
    public String verifyOtp(VerifyOtpRequestDTO request) {

        String email = normalizeEmail(request.getEmail());

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or expired OTP"
                ));

        if (!user.isPasswordSetupCompleted()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired OTP"
            );
        }

        PasswordResetOtpEntity otpEntity =
                otpRepository
                        .findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(
                                user.getUserId()
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid or expired OTP"
                        ));

        if (otpEntity.isUsed()
                || !otpEntity.getExpiresAt().isAfter(LocalDateTime.now())
                || otpEntity.getFailedAttempts() >= MAX_OTP_ATTEMPTS) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired OTP"
            );
        }

        if (!passwordEncoder.matches(
                request.getOtp(),
                otpEntity.getOtpHash())) {

            // Preserve independent failed-attempt tracking.
            otpAttemptService.incrementFailedAttempts(
                    otpEntity.getOtpId()
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid or expired OTP"
            );
        }

        // OTP must not be reusable.
        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        // Generate an unpredictable reset token.
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawResetToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        PasswordResetTokenEntity resetToken =
                new PasswordResetTokenEntity();

        resetToken.setUser(user);
        resetToken.setTokenHash(sha256(rawResetToken));
        resetToken.setExpiresAt(
                LocalDateTime.now().plusMinutes(10)
        );
        resetToken.setUsed(false);

        resetTokenRepository.deleteByUserUserId(user.getUserId());
        resetTokenRepository.save(resetToken);

        // Return raw token to the frontend only.
        return rawResetToken;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to process reset token",
                    ex
            );
        }
    }
}