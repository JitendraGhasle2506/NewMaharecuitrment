package com.maharecruitment.gov.in.attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.maharecruitment.gov.in.attendance.entity.AttendanceRequestAuditLogEntity;

public interface AttendanceRequestAuditLogRepository extends JpaRepository<AttendanceRequestAuditLogEntity, Long> {

    List<AttendanceRequestAuditLogEntity> findByRequestTypeAndRequestIdOrderByOccurredAtDescIdDesc(
            String requestType, Long requestId);
}
