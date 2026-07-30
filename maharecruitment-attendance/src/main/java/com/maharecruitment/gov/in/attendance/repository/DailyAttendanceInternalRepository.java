package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.DailyAttendanceInternalEntity;

@Repository
public interface DailyAttendanceInternalRepository extends JpaRepository<DailyAttendanceInternalEntity, Long> {

    List<DailyAttendanceInternalEntity> findByAttendanceDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyAttendanceInternalEntity> findByEmployeeIdAndAttendanceDateBetween(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate);

    List<DailyAttendanceInternalEntity> findByEmployeeIdInAndAttendanceDateBetween(
            Collection<Long> employeeIds,
            LocalDate startDate,
            LocalDate endDate);

    Optional<DailyAttendanceInternalEntity> findByEmployeeIdAndAttendanceDate(Long employeeId, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attendance
            from DailyAttendanceInternalEntity attendance
            where attendance.attendanceDate between :startDate and :endDate
              and (
                    attendance.employeeId = :employeeId
                    or (
                        :employeeCode is not null
                        and upper(attendance.employeeCode) = upper(:employeeCode)
                    )
              )
            """)
    List<DailyAttendanceInternalEntity> findByEmployeeIdentityAndAttendanceDateBetweenForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("employeeCode") String employeeCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attendance
            from DailyAttendanceInternalEntity attendance
            where attendance.attendanceDate = :attendanceDate
              and (
                    attendance.employeeId = :employeeId
                    or (
                        :employeeCode is not null
                        and upper(attendance.employeeCode) = upper(:employeeCode)
                    )
              )
            order by attendance.id desc
            """)
    List<DailyAttendanceInternalEntity> findByEmployeeIdentityAndAttendanceDateForUpdate(
            @Param("employeeId") Long employeeId,
            @Param("employeeCode") String employeeCode,
            @Param("attendanceDate") LocalDate attendanceDate);

    Optional<DailyAttendanceInternalEntity> findFirstByEmployeeIdAndAttendanceDateOrderByIdDesc(
            Long employeeId,
            LocalDate date);

    List<DailyAttendanceInternalEntity> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    long countByAttendanceDateAndStatusIgnoreCase(LocalDate attendanceDate, String status);
}
