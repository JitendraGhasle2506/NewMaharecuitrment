package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Query(value = """
            with normalized_attendance as (
                select attendance.employee_id,
                       attendance.status,
                       attendance.check_in_time,
                       attendance.check_out_time,
                       case
                           when trim(coalesce(attendance.in_time, ''))
                                ~ '^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9]([.][0-9]+)?)?$'
                           then cast(trim(attendance.in_time) as time)
                           else null
                       end as api_check_in_time,
                       case
                           when trim(coalesce(attendance.out_time, ''))
                                ~ '^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9]([.][0-9]+)?)?$'
                           then cast(trim(attendance.out_time) as time)
                           else null
                       end as api_check_out_time
                from daily_attendance_internal_employee attendance
                where attendance.attendance_date = :attendanceDate
            ), attendance_rows as (
                select employee_id,
                       upper(trim(coalesce(status, ''))) = 'PRESENT' as marked_present,
                       least(check_in_time, check_out_time, api_check_in_time, api_check_out_time)
                           as first_event_time
                from normalized_attendance
            ), effective_attendance as (
                select employee_id,
                       bool_or(marked_present) as marked_present,
                       min(first_event_time) as effective_check_in_time
                from attendance_rows
                group by employee_id
            )
            select count(*) filter (
                       where marked_present or effective_check_in_time is not null
                   ) as presentCount,
                   count(*) filter (
                       where effective_check_in_time is not null
                   ) as checkedInCount,
                   count(*) filter (
                       where effective_check_in_time < cast(:earlyCutoff as time)
                   ) as earlyCount,
                   count(*) filter (
                       where effective_check_in_time >= cast(:earlyCutoff as time)
                         and effective_check_in_time <= cast(:lateCutoff as time)
                   ) as standardCount,
                   count(*) filter (
                       where effective_check_in_time > cast(:lateCutoff as time)
                   ) as lateCount
            from effective_attendance
            """, nativeQuery = true)
    AttendanceCheckInSummaryProjection summarizeAttendanceByDate(
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("earlyCutoff") LocalTime earlyCutoff,
            @Param("lateCutoff") LocalTime lateCutoff);
}
