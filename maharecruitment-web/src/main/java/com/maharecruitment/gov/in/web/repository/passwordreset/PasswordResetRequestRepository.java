package com.maharecruitment.gov.in.web.repository.passwordreset;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.web.entity.passwordreset.PasswordResetRequestEntity;
import com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetStatus;
import com.maharecruitment.gov.in.web.service.passwordreset.ResetPasswordChannel;

import jakarta.persistence.LockModeType;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequestEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetRequestEntity> findFirstByUser_IdAndChannelAndRequestStatusInOrderByCreatedOnDesc(
            Long userId,
            ResetPasswordChannel channel,
            Collection<PasswordResetStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetRequestEntity> findByResetTokenHash(String resetTokenHash);

    @Modifying
    @Query("""
            update PasswordResetRequestEntity request
               set request.requestStatus = :cancelledStatus,
                   request.updatedOn = :updatedOn
             where request.user.id = :userId
               and request.requestStatus in :activeStatuses
               and (:excludedRequestId is null or request.id <> :excludedRequestId)
            """)
    int cancelActiveRequestsForUser(
            @Param("userId") Long userId,
            @Param("activeStatuses") Collection<PasswordResetStatus> activeStatuses,
            @Param("cancelledStatus") PasswordResetStatus cancelledStatus,
            @Param("updatedOn") Instant updatedOn,
            @Param("excludedRequestId") Long excludedRequestId);

    @Modifying
    @Query("""
            update PasswordResetRequestEntity request
               set request.requestStatus = :expiredStatus,
                   request.updatedOn = :updatedOn
             where request.requestStatus in :activeStatuses
               and (
                    (request.requestStatus = com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetStatus.OTP_SENT
                     and request.otpExpiryTime <= :updatedOn)
                 or (request.requestStatus = com.maharecruitment.gov.in.web.service.passwordreset.PasswordResetStatus.OTP_VERIFIED
                     and request.resetTokenExpiryTime <= :updatedOn)
               )
            """)
    int expireRequests(
            @Param("activeStatuses") Collection<PasswordResetStatus> activeStatuses,
            @Param("expiredStatus") PasswordResetStatus expiredStatus,
            @Param("updatedOn") Instant updatedOn);
}
