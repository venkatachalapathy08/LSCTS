
package com.project.lscts.controller;

import com.project.lscts.dto.auth.ForgotPasswordRequestDTO;
import com.project.lscts.dto.auth.LoginRequestDTO;
import com.project.lscts.dto.auth.LoginResponseDTO;
import com.project.lscts.dto.auth.ResetPasswordRequestDTO;
import com.project.lscts.dto.auth.VerifyOtpRequestDTO;
import com.project.lscts.dto.user.SetupPasswordRequestDTO;
import com.project.lscts.entity.UserEntity;
import com.project.lscts.security.JwtService;
import com.project.lscts.service.AuthService;
import com.project.lscts.service.PasswordSetupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordSetupService passwordSetupService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            PasswordSetupService passwordSetupService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.passwordSetupService = passwordSetupService;
        this.jwtService=jwtService;
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Request password-reset OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request
    ) {
        return ResponseEntity.ok(
                authService.forgotPassword(request)
        );
    }

    // Verify OTP and receive reset token
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(
            @Valid @RequestBody VerifyOtpRequestDTO request
    ) {
        return ResponseEntity.ok(
                authService.verifyOtp(request)
        );
    }

    // Reset password using reset token
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        return ResponseEntity.ok(
                authService.resetPassword(request)
        );
    }

    // Set initial password using resident setup token
    @PostMapping("/setup-password")
    public ResponseEntity<LoginResponseDTO> setupPassword(
            @Valid @RequestBody SetupPasswordRequestDTO request
    ) {
        UserEntity user = passwordSetupService.setupPassword(request);

        String jwt = jwtService.generateToken(user);

        LoginResponseDTO response = new LoginResponseDTO(
                user.getUserId(),
                user.getName(),
                jwt,
                user.getRole()
        );

        return ResponseEntity.ok(response);
    }
}
