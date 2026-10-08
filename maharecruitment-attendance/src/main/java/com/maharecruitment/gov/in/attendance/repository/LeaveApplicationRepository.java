package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface LeaveApplicationRepository extends JpaRepository<LeaveApplicationEntity, Long> {

    List<LeaveApplicationEntity> findByEmployeeIdOrderByApplicationDateDesc(Long employeeId);

    List<LeaveApplicationEntity> findByEmployeeIdInAndStatusOrderByApplicationDateDesc(List<Long> employeeIds, String status);
    
    List<LeaveApplicationEntity> findByEmployeeIdInAndStatusInOrderByApplicationDateDesc(List<Long> employeeIds, List<String> statuses);

    @Query("""
            select leave from LeaveApplicationEntity leave
            where leave.status = :status
              and (
                    (leave.approvalStage = 'MANAGER' and leave.managerApproverUserId = :actorUserId)
                 or (leave.approvalStage = 'HOD' and leave.hodApproverUserId = :actorUserId)
                 or ((leave.approvalStage is null or leave.approvalStage not in ('MANAGER', 'HOD'))
                     and leave.employeeId in :legacyEmployeeIds)
              )
            order by leave.applicationDate desc
            """)
    List<LeaveApplicationEntity> findPendingForApprover(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("status") String status);

    @Query("""
            select leave from LeaveApplicationEntity leave
            join EmployeeEntity employee on employee.employeeId = leave.employeeId
            left join employee.designation designation
            where leave.status = 'PENDING'
              and (
                    (leave.approvalStage = 'MANAGER' and leave.managerApproverUserId = :actorUserId)
                 or (leave.approvalStage = 'HOD' and leave.hodApproverUserId = :actorUserId)
                 or ((leave.approvalStage is null or leave.approvalStage not in ('MANAGER', 'HOD'))
                     and leave.employeeId in :legacyEmployeeIds)
              )
              and (:searchPattern is null
                   or upper(employee.fullName) like :searchPattern
                   or upper(employee.employeeCode) like :searchPattern
                   or upper(designation.designationName) like :searchPattern)
              and (:searchDate is null
                   or (leave.startDate <= :searchDate and leave.endDate >= :searchDate)
                   or leave.compOffWorkDate = :searchDate)
            order by leave.applicationDate desc
            """)
    List<LeaveApplicationEntity> findPendingForApproverFiltered(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("searchPattern") String searchPattern,
            @Param("searchDate") LocalDate searchDate);

    @Query("""
            select leave from LeaveApplicationEntity leave
            where leave.status in :statuses
              and (
                    leave.managerApproverUserId = :actorUserId
                 or leave.hodApproverUserId = :actorUserId
                 or ((leave.approvalStage is null or leave.approvalStage not in ('MANAGER', 'HOD'))
                     and leave.employeeId in :legacyEmployeeIds)
              )
            order by leave.applicationDate desc
            """)
    List<LeaveApplicationEntity> findProcessedForApprover(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("statuses") Collection<String> statuses);

    @Query(value = """
            select leave from LeaveApplicationEntity leave
            join EmployeeEntity employee on employee.employeeId = leave.employeeId
            left join employee.designation designation
            where leave.status in ('APPROVED', 'REJECTED')
              and (
                    leave.managerApproverUserId = :actorUserId
                 or leave.hodApproverUserId = :actorUserId
                 or ((leave.approvalStage is null or leave.approvalStage not in ('MANAGER', 'HOD'))
                     and leave.employeeId in :legacyEmployeeIds)
              )
              and (:searchPattern is null
                   or upper(employee.fullName) like :searchPattern
                   or upper(employee.employeeCode) like :searchPattern
                   or upper(designation.designationName) like :searchPattern)
              and (:searchDate is null
                   or (leave.startDate <= :searchDate and leave.endDate >= :searchDate)
                   or leave.compOffWorkDate = :searchDate)
            order by leave.applicationDate desc
            """,
            countQuery = """
            select count(leave) from LeaveApplicationEntity leave
            join EmployeeEntity employee on employee.employeeId = leave.employeeId
            left join employee.designation designation
            where leave.status in ('APPROVED', 'REJECTED')
              and (
                    leave.managerApproverUserId = :actorUserId
                 or leave.hodApproverUserId = :actorUserId
                 or ((leave.approvalStage is null or leave.approvalStage not in ('MANAGER', 'HOD'))
                     and leave.employeeId in :legacyEmployeeIds)
              )
              and (:searchPattern is null
                   or upper(employee.fullName) like :searchPattern
                   or upper(employee.employeeCode) like :searchPattern
                   or upper(designation.designationName) like :searchPattern)
              and (:searchDate is null
                   or (leave.startDate <= :searchDate and leave.endDate >= :searchDate)
                   or leave.compOffWorkDate = :searchDate)
            """)
    Page<LeaveApplicationEntity> findProcessedForApproverPage(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("searchPattern") String searchPattern,
            @Param("searchDate") LocalDate searchDate,
            Pageable pageable);

    List<LeaveApplicationEntity> findByEmployeeIdAndStatus(Long employeeId, String status);

    List<LeaveApplicationEntity> findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            String status,
            LocalDate endDate,
            LocalDate startDate);

    @Query("select leave from LeaveApplicationEntity leave "
            + "where leave.employeeId in :employeeIds "
            + "and upper(leave.status) = 'APPROVED' "
            + "and leave.startDate <= :endDate "
            + "and leave.endDate >= :startDate")
    List<LeaveApplicationEntity> findApprovedOverlappingPeriod(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByEmployeeIdAndCompOffWorkDateAndStatusIn(
            Long employeeId,
            LocalDate compOffWorkDate,
            Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select leave from LeaveApplicationEntity leave where leave.leaveId = :leaveId")
    Optional<LeaveApplicationEntity> findByLeaveIdForUpdate(@Param("leaveId") Long leaveId);
}
