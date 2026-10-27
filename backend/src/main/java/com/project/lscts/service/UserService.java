package com.project.lscts.service;

import com.project.lscts.dto.user.UserResponseDTO;
import com.project.lscts.entity.Role;
import com.project.lscts.entity.UserEntity;
import com.project.lscts.repository.ComplaintRepository;
import com.project.lscts.repository.PasswordSetupTokenRepository;
import com.project.lscts.repository.StatusHistoryRepository;
import com.project.lscts.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;

    // =========================================
    // ADMIN: Get all users
    // =========================================
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    // =========================================
    // ADMIN: Get user by ID
    // =========================================
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with ID: " + userId
                ));

        return convertToDTO(user);
    }

    // =========================================
    // USER / ADMIN: Get logged-in user's profile
    // =========================================
    @Transactional(readOnly = true)
    public UserResponseDTO getUserByEmail(String email) {

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return convertToDTO(user);
    }

    // =========================================
    // ADMIN: Permanently delete a user
    // =========================================
    @Transactional
    public void deleteUserPermanently(Long userId) {

        // Step 1: Find the user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found with ID: " + userId
                ));

        // Step 2: Prevent deleting the admin account
        if (user.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin account cannot be deleted"
            );
        }

        // Step 3: Delete password setup tokens first
        passwordSetupTokenRepository.deleteByUserUserId(userId);

        // Step 4: Delete status history for this user's complaints
        statusHistoryRepository.deleteAllByComplaintOwnerId(userId);

        // Step 5: Delete the user's complaints
        complaintRepository.deleteAllByUserId(userId);

        // Step 6: Delete the user
        userRepository.delete(user);
    }

    // =========================================
    // Convert UserEntity to UserResponseDTO
    // =========================================
    private UserResponseDTO convertToDTO(UserEntity user) {

        return new UserResponseDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isPasswordSetupCompleted(),
                user.getUserCreatedAt()
        );
    }
}
