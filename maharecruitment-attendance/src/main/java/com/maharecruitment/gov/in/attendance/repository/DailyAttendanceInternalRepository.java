package com.maharecruitment.gov.in.attendance.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
                join employee_master employee on employee.employee_id = attendance.employee_id
                where attendance.attendance_date = :attendanceDate
                  and upper(trim(coalesce(employee.status, ''))) = 'ACTIVE'
                  and upper(trim(coalesce(employee.recruitment_type, ''))) = 'INTERNAL'
                  and trim(coalesce(employee.employee_code, '')) <> ''
                  and upper(trim(coalesce(employee.employee_code, ''))) <> 'PENDING'
                  and upper(trim(coalesce(employee.employee_code, ''))) not like 'TMP-%'
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
                         and effective_check_in_time < cast(:lateCutoff as time)
                   ) as standardCount,
                   count(*) filter (
                       where effective_check_in_time >= cast(:lateCutoff as time)
                         and effective_check_in_time <= cast(:afterElevenCutoff as time)
                   ) as lateCount,
                   count(*) filter (
                       where effective_check_in_time > cast(:afterElevenCutoff as time)
                   ) as afterElevenCount
            from effective_attendance
            """, nativeQuery = true)
    AttendanceCheckInSummaryProjection summarizeAttendanceByDate(
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("earlyCutoff") LocalTime earlyCutoff,
            @Param("lateCutoff") LocalTime lateCutoff,
            @Param("afterElevenCutoff") LocalTime afterElevenCutoff);

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
            ), external_present as (
                select distinct attendance.user_id as employee_id
                from attendance_daily attendance
                where attendance.month = :month
                  and attendance.year = :year
                  and upper(trim(coalesce(
                        case :day
                            when 1 then attendance.d1
                            when 2 then attendance.d2
                            when 3 then attendance.d3
                            when 4 then attendance.d4
                            when 5 then attendance.d5
                            when 6 then attendance.d6
                            when 7 then attendance.d7
                            when 8 then attendance.d8
                            when 9 then attendance.d9
                            when 10 then attendance.d10
                            when 11 then attendance.d11
                            when 12 then attendance.d12
                            when 13 then attendance.d13
                            when 14 then attendance.d14
                            when 15 then attendance.d15
                            when 16 then attendance.d16
                            when 17 then attendance.d17
                            when 18 then attendance.d18
                            when 19 then attendance.d19
                            when 20 then attendance.d20
                            when 21 then attendance.d21
                            when 22 then attendance.d22
                            when 23 then attendance.d23
                            when 24 then attendance.d24
                            when 25 then attendance.d25
                            when 26 then attendance.d26
                            when 27 then attendance.d27
                            when 28 then attendance.d28
                            when 29 then attendance.d29
                            when 30 then attendance.d30
                            when 31 then attendance.d31
                        end, ''))) in ('P', 'PRESENT')
            ), employee_attendance as (
                select employee.employee_id,
                       employee.employee_code,
                       employee.full_name,
                       upper(trim(coalesce(employee.recruitment_type, ''))) as recruitment_type,
                       internal_attendance.effective_check_in_time,
                       cell_mapping.cell_id,
                       case
                           when upper(trim(coalesce(employee.recruitment_type, ''))) = 'INTERNAL'
                                and (
                                    coalesce(internal_attendance.marked_present, false)
                                    or internal_attendance.effective_check_in_time is not null
                                )
                           then true
                           when upper(trim(coalesce(employee.recruitment_type, ''))) = 'EXTERNAL'
                                and external_attendance.employee_id is not null
                           then true
                           else false
                       end as present
                from employee_master employee
                left join effective_attendance internal_attendance
                       on internal_attendance.employee_id = employee.employee_id
                left join external_present external_attendance
                       on external_attendance.employee_id = employee.employee_id
                left join employee_cell_mapping cell_mapping
                       on cell_mapping.employee_id = employee.employee_id
                where upper(trim(coalesce(employee.status, ''))) = 'ACTIVE'
                  and trim(coalesce(employee.employee_code, '')) <> ''
                  and upper(trim(coalesce(employee.employee_code, ''))) <> 'PENDING'
                  and upper(trim(coalesce(employee.employee_code, ''))) not like 'TMP-%'
            )
            select employee_id as employeeId,
                   employee_code as employeeCode,
                   full_name as fullName,
                   recruitment_type as recruitmentType,
                   case when present then 'PRESENT' else 'ABSENT' end as attendanceStatus,
                   effective_check_in_time as checkInTime
            from employee_attendance
            where (:cellId is null or cell_id = :cellId)
              and (
                    :category = 'TOTAL'
                    or (:category = 'PRESENT' and present)
                    or (:category = 'ABSENT' and not present)
                    or (:category = 'CHECKED_IN' and effective_check_in_time is not null)
                    or (:category = 'EARLY'
                        and effective_check_in_time < cast(:earlyCutoff as time))
                    or (:category = 'STANDARD'
                        and effective_check_in_time >= cast(:earlyCutoff as time)
                        and effective_check_in_time < cast(:lateCutoff as time))
                    or (:category = 'LATE'
                        and effective_check_in_time >= cast(:lateCutoff as time)
                        and effective_check_in_time <= cast(:afterElevenCutoff as time))
                    or (:category = 'AFTER_ELEVEN'
                        and effective_check_in_time > cast(:afterElevenCutoff as time))
              )
            order by case when effective_check_in_time is null then 1 else 0 end,
                     effective_check_in_time,
                     lower(full_name),
                     employee_id
            """, nativeQuery = true)
    Slice<AttendanceEmployeeDetailProjection> findAttendanceEmployeeDetails(
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("day") Integer day,
            @Param("category") String category,
            @Param("cellId") Long cellId,
            @Param("earlyCutoff") LocalTime earlyCutoff,
            @Param("lateCutoff") LocalTime lateCutoff,
            @Param("afterElevenCutoff") LocalTime afterElevenCutoff,
            Pageable pageable);

    @Query(value = """
            with internal_present as (
                select distinct attendance.employee_id
                from daily_attendance_internal_employee attendance
                where attendance.attendance_date = :attendanceDate
                  and (
                        upper(trim(coalesce(attendance.status, ''))) = 'PRESENT'
                        or attendance.check_in_time is not null
                        or attendance.check_out_time is not null
                        or trim(coalesce(attendance.in_time, ''))
                            ~ '^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9]([.][0-9]+)?)?$'
                        or trim(coalesce(attendance.out_time, ''))
                            ~ '^([01]?[0-9]|2[0-3]):[0-5][0-9](:[0-5][0-9]([.][0-9]+)?)?$'
                  )
            ), external_present as (
                select distinct attendance.user_id as employee_id
                from attendance_daily attendance
                where attendance.month = :month
                  and attendance.year = :year
                  and upper(trim(coalesce(
                        case :day
                            when 1 then attendance.d1
                            when 2 then attendance.d2
                            when 3 then attendance.d3
                            when 4 then attendance.d4
                            when 5 then attendance.d5
                            when 6 then attendance.d6
                            when 7 then attendance.d7
                            when 8 then attendance.d8
                            when 9 then attendance.d9
                            when 10 then attendance.d10
                            when 11 then attendance.d11
                            when 12 then attendance.d12
                            when 13 then attendance.d13
                            when 14 then attendance.d14
                            when 15 then attendance.d15
                            when 16 then attendance.d16
                            when 17 then attendance.d17
                            when 18 then attendance.d18
                            when 19 then attendance.d19
                            when 20 then attendance.d20
                            when 21 then attendance.d21
                            when 22 then attendance.d22
                            when 23 then attendance.d23
                            when 24 then attendance.d24
                            when 25 then attendance.d25
                            when 26 then attendance.d26
                            when 27 then attendance.d27
                            when 28 then attendance.d28
                            when 29 then attendance.d29
                            when 30 then attendance.d30
                            when 31 then attendance.d31
                        end, ''))) in ('P', 'PRESENT')
            )
            select cell.cell_id as cellId,
                   cell.cell_name as cellName,
                   wing.wing_name as wingName,
                   count(distinct employee.employee_id) as totalEmployees,
                   count(distinct employee.employee_id) filter (
                       where (
                           upper(trim(coalesce(employee.recruitment_type, ''))) = 'INTERNAL'
                           and ip.employee_id is not null
                       ) or (
                           upper(trim(coalesce(employee.recruitment_type, ''))) = 'EXTERNAL'
                           and ep.employee_id is not null
                       )
                   ) as presentEmployees
            from m_cell_master cell
            join m_wing_master wing on wing.wing_id = cell.wing_id
            left join employee_cell_mapping mapping on mapping.cell_id = cell.cell_id
            left join employee_master employee
                   on employee.employee_id = mapping.employee_id
                  and upper(trim(coalesce(employee.status, ''))) = :employeeStatus
                  and trim(coalesce(employee.employee_code, '')) <> ''
                  and upper(trim(coalesce(employee.employee_code, ''))) <> 'PENDING'
                  and upper(trim(coalesce(employee.employee_code, ''))) not like 'TMP-%'
            left join internal_present ip on ip.employee_id = employee.employee_id
            left join external_present ep on ep.employee_id = employee.employee_id
            where upper(coalesce(cell.active_flag, 'N')) = :activeFlag
              and upper(coalesce(wing.active_flag, 'N')) = :activeFlag
            group by wing.wing_name, cell.cell_id, cell.cell_name
            order by lower(wing.wing_name), lower(cell.cell_name), cell.cell_id
            """, nativeQuery = true)
    List<AttendanceCellSummaryProjection> summarizeAttendanceByCell(
            @Param("attendanceDate") LocalDate attendanceDate,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("day") Integer day,
            @Param("activeFlag") String activeFlag,
            @Param("employeeStatus") String employeeStatus);
}
