package com.maharecruitment.gov.in.web.repository.verification;

import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.web.entity.verification.OtpVerificationStateEntity;

import jakarta.persistence.LockModeType;

public interface OtpVerificationStateRepository extends JpaRepository<OtpVerificationStateEntity, Long> {

    Optional<OtpVerificationStateEntity> findBySessionIdAndPurposeAndChannel(
            String sessionId,
            String purpose,
            String channel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select state
            from OtpVerificationStateEntity state
            where state.sessionId = :sessionId
              and state.purpose = :purpose
              and state.channel = :channel
            """)
    Optional<OtpVerificationStateEntity> findForUpdate(
            @Param("sessionId") String sessionId,
            @Param("purpose") String purpose,
            @Param("channel") String channel);

    void deleteBySessionIdAndPurpose(String sessionId, String purpose);

    void deleteBySessionIdAndPurposeAndChannel(String sessionId, String purpose, String channel);
}
