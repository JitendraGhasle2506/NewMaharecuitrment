package com.maharecruitment.gov.in.attendance.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.maharecruitment.gov.in.attendance.dto.InternalDecisionLogView;
import com.maharecruitment.gov.in.attendance.entity.AttendanceRequestAuditLogEntity;

public interface AttendanceRequestAuditLogRepository extends JpaRepository<AttendanceRequestAuditLogEntity, Long> {

    List<AttendanceRequestAuditLogEntity> findByRequestTypeAndRequestIdOrderByOccurredAtDescIdDesc(
            String requestType, Long requestId);

    /** Approve, reject and forward events on requests of employees whose current recruitment type is INTERNAL. */
    @Query(value = """
            select new com.maharecruitment.gov.in.attendance.dto.InternalDecisionLogView(
                a.occurredAt, a.requestType, a.requestId, e.employeeCode, e.fullName, a.actionType,
                a.actorName, a.actorLoginId, a.actorRoles, a.remarks, a.channel, a.ipAddress)
            from AttendanceRequestAuditLogEntity a
            join EmployeeEntity e on e.employeeId = a.employeeId
            where upper(trim(e.recruitmentType)) = 'INTERNAL'
              and a.actionType in ('APPROVE', 'REJECT', 'FORWARD')
            order by a.occurredAt desc, a.id desc
            """,
            countQuery = """
            select count(a)
            from AttendanceRequestAuditLogEntity a
            join EmployeeEntity e on e.employeeId = a.employeeId
            where upper(trim(e.recruitmentType)) = 'INTERNAL'
              and a.actionType in ('APPROVE', 'REJECT', 'FORWARD')
            """)
    Page<InternalDecisionLogView> findInternalEmployeeDecisions(Pageable pageable);
}
