package com.maharecruitment.gov.in.common.mahaitprofile.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfileAuditLog;

public interface MahaItProfileAuditLogRepository extends JpaRepository<MahaItProfileAuditLog, Long> {

    @Query("SELECT a FROM MahaItProfileAuditLog a WHERE a.mahaItProfileId = :mahaItProfileId ORDER BY a.actionTimestamp DESC")
    List<MahaItProfileAuditLog> findByMahaItProfileIdOrderByActionTimestampDesc(
            @Param("mahaItProfileId") Long mahaItProfileId);
}
