package com.white.handdam.auth.repository;

import com.white.handdam.auth.entity.EmailVerification;
import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    // PENDING 토큰 하나만 유지하기 위함
    List<EmailVerification> findAllByEmailAndPurposeAndStatus(
            String email, VerificationPurpose purpose, VerificationStatus status);

}
