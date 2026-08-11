package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    List<LeaveApplicationEntity> findByEmployeeIdAndStatus(Long employeeId, String status);

    List<LeaveApplicationEntity> findByEmployeeIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId,
            String status,
            LocalDate endDate,
            LocalDate startDate);

    boolean existsByEmployeeIdAndCompOffWorkDateAndStatusIn(
            Long employeeId,
            LocalDate compOffWorkDate,
            Collection<String> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select leave from LeaveApplicationEntity leave where leave.leaveId = :leaveId")
    Optional<LeaveApplicationEntity> findByLeaveIdForUpdate(@Param("leaveId") Long leaveId);
}
