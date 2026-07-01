package com.maharecruitment.gov.in.web.repository.verification;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.web.entity.verification.OtpVerificationStateEntity;

public interface OtpVerificationStateRepository extends JpaRepository<OtpVerificationStateEntity, Long> {

    Optional<OtpVerificationStateEntity> findBySessionIdAndPurposeAndChannel(
            String sessionId,
            String purpose,
            String channel);

    void deleteBySessionIdAndPurpose(String sessionId, String purpose);

    void deleteBySessionIdAndPurposeAndChannel(String sessionId, String purpose, String channel);
}
