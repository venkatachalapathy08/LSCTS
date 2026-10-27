
package com.project.lscts.service;

import com.project.lscts.entity.PasswordResetOtpEntity;
import com.project.lscts.repository.PasswordResetOtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpAttemptService {

    private final PasswordResetOtpRepository otpRepository;

    public OtpAttemptService(PasswordResetOtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementFailedAttempts(Long otpId) {

        PasswordResetOtpEntity otp = otpRepository.findById(otpId)
                .orElseThrow(() -> new IllegalStateException(
                        "OTP record not found"
                ));

        otp.setFailedAttempts(otp.getFailedAttempts() + 1);

        otpRepository.save(otp);
    }
}
