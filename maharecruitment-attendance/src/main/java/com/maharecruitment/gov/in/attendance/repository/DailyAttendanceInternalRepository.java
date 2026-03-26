package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;

@Repository
public interface DailyAttendanceInternalRepository extends JpaRepository<DailyAttendanceInternalEntity, Long> {

    List<DailyAttendanceInternalEntity> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyAttendanceInternalEntity> findByEmployeeIdAndAttendanceDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate);
    Optional<DailyAttendanceInternalEntity> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    List<DailyAttendanceInternalEntity> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);
}
