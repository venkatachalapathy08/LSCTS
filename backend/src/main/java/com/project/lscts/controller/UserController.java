package com.project.lscts.controller;

import com.project.lscts.dto.user.AdminCreateUserRequestDTO;
import com.project.lscts.dto.user.UserResponseDTO;
import com.project.lscts.service.PasswordSetupService;
import com.project.lscts.service.UserService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordSetupService passwordSetupService;

    // =========================================
    // USER + ADMIN: Get logged-in user's profile
    // GET /api/users/me
    // =========================================
    @GetMapping("/api/users/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDTO> getMyProfile(
            Authentication authentication) {

        String email = authentication.getName();

        UserResponseDTO profile =
                userService.getUserByEmail(email);

        return ResponseEntity.ok(profile);
    }

    // =========================================
    // ADMIN: Get all users
    // GET /api/admin/users
    // =========================================
    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    // =========================================
    // ADMIN: Get user by ID
    // GET /api/admin/users/{userId}
    // =========================================
    @GetMapping("/api/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                userService.getUserById(userId)
        );
    }

    // =========================================
    // ADMIN: Create resident account
    // POST /api/admin/users
    // =========================================
    @PostMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createResident(
            @Valid @RequestBody AdminCreateUserRequestDTO request) {

        passwordSetupService.createResident(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        "Resident account created. " +
                                "Password setup instructions have been sent by email."
                );
    }

    // =========================================
    // ADMIN: Delete resident account
    // DELETE /api/admin/users/{userId}
    // =========================================
    @DeleteMapping("/api/admin/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId) {

        userService.deleteUserPermanently(userId);

        return ResponseEntity.noContent().build();
    }

    // =========================================
    // ADMIN: Cancel pending resident account
    // DELETE /api/admin/users/{userId}/pending
    // =========================================
    @DeleteMapping("/api/admin/users/{userId}/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> cancelResident(
            @PathVariable Long userId) {

        passwordSetupService.cancelPendingResident(userId);

        return ResponseEntity.ok(
                "Pending resident account cancelled successfully."
        );
    }
}
