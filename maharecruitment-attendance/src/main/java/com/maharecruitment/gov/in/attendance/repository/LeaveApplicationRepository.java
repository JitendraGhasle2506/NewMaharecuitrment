package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.LeaveApplicationEntity;

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
}
