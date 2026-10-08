package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;

import jakarta.persistence.LockModeType;

@Repository
public interface TourApplicationRepository extends JpaRepository<TourApplicationEntity, Long> {
    List<TourApplicationEntity> findByEmployeeIdOrderByApplicationDateDesc(Long employeeId);
    List<TourApplicationEntity> findByEmployeeIdInAndStatusOrderByApplicationDateDesc(List<Long> employeeIds, String status);
    
    List<TourApplicationEntity> findByEmployeeIdInAndStatusInOrderByApplicationDateDesc(List<Long> employeeIds, List<String> statuses);

    @Query("""
            select tour from TourApplicationEntity tour
            where tour.status = :status
              and (
                    (tour.approvalStage = 'MANAGER' and tour.managerApproverUserId = :actorUserId)
                 or (tour.approvalStage = 'HOD' and tour.hodApproverUserId = :actorUserId)
                 or ((tour.approvalStage is null or tour.approvalStage not in ('MANAGER', 'HOD'))
                     and tour.employeeId in :legacyEmployeeIds)
              )
            order by tour.applicationDate desc
            """)
    List<TourApplicationEntity> findPendingForApprover(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("status") String status);

    @Query("""
            select tour from TourApplicationEntity tour
            join EmployeeEntity employee on employee.employeeId = tour.employeeId
            left join employee.designation designation
            where tour.status = 'PENDING'
              and (
                    (tour.approvalStage = 'MANAGER' and tour.managerApproverUserId = :actorUserId)
                 or (tour.approvalStage = 'HOD' and tour.hodApproverUserId = :actorUserId)
                 or ((tour.approvalStage is null or tour.approvalStage not in ('MANAGER', 'HOD'))
                     and tour.employeeId in :legacyEmployeeIds)
              )
              and (:searchPattern is null
                   or upper(employee.fullName) like :searchPattern
                   or upper(employee.employeeCode) like :searchPattern
                   or upper(designation.designationName) like :searchPattern)
              and (:searchDate is null
                   or (tour.startDate <= :searchDate and tour.endDate >= :searchDate))
            order by tour.applicationDate desc
            """)
    List<TourApplicationEntity> findPendingForApproverFiltered(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("searchPattern") String searchPattern,
            @Param("searchDate") LocalDate searchDate);

    @Query("""
            select tour from TourApplicationEntity tour
            where tour.status in :statuses
              and (
                    tour.managerApproverUserId = :actorUserId
                 or tour.hodApproverUserId = :actorUserId
                 or ((tour.approvalStage is null or tour.approvalStage not in ('MANAGER', 'HOD'))
                     and tour.employeeId in :legacyEmployeeIds)
              )
            order by tour.applicationDate desc
            """)
    List<TourApplicationEntity> findProcessedForApprover(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("statuses") Collection<String> statuses);

    @Query(value = """
            select tour from TourApplicationEntity tour
            join EmployeeEntity employee on employee.employeeId = tour.employeeId
            left join employee.designation designation
            where tour.status in ('APPROVED', 'REJECTED')
              and (
                    tour.managerApproverUserId = :actorUserId
                 or tour.hodApproverUserId = :actorUserId
                 or ((tour.approvalStage is null or tour.approvalStage not in ('MANAGER', 'HOD'))
                     and tour.employeeId in :legacyEmployeeIds)
              )
              and (:searchPattern is null
                   or upper(employee.fullName) like :searchPattern
                   or upper(employee.employeeCode) like :searchPattern
                   or upper(designation.designationName) like :searchPattern)
              and (:searchDate is null
                   or (tour.startDate <= :searchDate and tour.endDate >= :searchDate))
            order by tour.applicationDate desc
            """,
            countQuery = """
            select count(tour) from TourApplicationEntity tour
            join EmployeeEntity employee on employee.employeeId = tour.employeeId
            left join employee.designation designation
            where tour.status in ('APPROVED', 'REJECTED')
              and (
                    tour.managerApproverUserId = :actorUserId
                 or tour.hodApproverUserId = :actorUserId
                 or ((tour.approvalStage is null or tour.approvalStage not in ('MANAGER', 'HOD'))
                     and tour.employeeId in :legacyEmployeeIds)
              )
              and (:searchPattern is null
                   or upper(employee.fullName) like :searchPattern
                   or upper(employee.employeeCode) like :searchPattern
                   or upper(designation.designationName) like :searchPattern)
              and (:searchDate is null
                   or (tour.startDate <= :searchDate and tour.endDate >= :searchDate))
            """)
    Page<TourApplicationEntity> findProcessedForApproverPage(
            @Param("actorUserId") Long actorUserId,
            @Param("legacyEmployeeIds") Collection<Long> legacyEmployeeIds,
            @Param("searchPattern") String searchPattern,
            @Param("searchDate") LocalDate searchDate,
            Pageable pageable);
    List<TourApplicationEntity> findByEmployeeIdAndStatus(Long employeeId, String status);

    List<TourApplicationEntity> findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            String status,
            LocalDate endDate,
            LocalDate startDate);

    @Query("select tour from TourApplicationEntity tour "
            + "where tour.employeeId in :employeeIds "
            + "and upper(tour.status) = 'APPROVED' "
            + "and tour.startDate <= :endDate "
            + "and tour.endDate >= :startDate")
    List<TourApplicationEntity> findApprovedOverlappingPeriod(
            @Param("employeeIds") Collection<Long> employeeIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    boolean existsByEmployeeIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            Collection<String> statuses,
            LocalDate startDate,
            LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tour from TourApplicationEntity tour where tour.tourId = :tourId")
    java.util.Optional<TourApplicationEntity> findByTourIdForUpdate(@Param("tourId") Long tourId);
}
