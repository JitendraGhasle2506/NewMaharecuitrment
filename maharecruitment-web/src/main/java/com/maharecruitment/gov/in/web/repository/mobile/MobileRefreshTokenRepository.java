package com.maharecruitment.gov.in.web.repository.mobile;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.web.entity.mobile.MobileRefreshTokenEntity;

import jakarta.persistence.LockModeType;

public interface MobileRefreshTokenRepository extends JpaRepository<MobileRefreshTokenEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MobileRefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update MobileRefreshTokenEntity token
               set token.revokedAt = :revokedAt
             where token.user.id = :userId
               and token.revokedAt is null
            """)
    int revokeActiveTokensForUser(@Param("userId") Long userId, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("delete from MobileRefreshTokenEntity token where token.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
