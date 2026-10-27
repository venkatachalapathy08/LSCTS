
package com.project.lscts.repository;

import com.project.lscts.entity.PasswordSetupTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordSetupTokenRepository
        extends JpaRepository<PasswordSetupTokenEntity, Long> {

    Optional<PasswordSetupTokenEntity> findByTokenHash(String tokenHash);

    void deleteByUserUserId(Long userId);
}
