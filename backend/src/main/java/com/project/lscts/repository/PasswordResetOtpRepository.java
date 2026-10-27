
package com.project.lscts.repository;

import com.project.lscts.entity.PasswordResetOtpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtpEntity, Long> {

    Optional<PasswordResetOtpEntity> findTopByUserUserIdAndUsedFalseOrderByCreatedAtDesc(
            Long userId
    );

    void deleteByUserUserId(Long userId);
}
