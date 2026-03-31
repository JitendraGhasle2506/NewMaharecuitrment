package com.maharecruitment.gov.in.attendance.repository;

import com.maharecruitment.gov.in.attendance.entity.ManualAttendanceRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ManualAttendanceRequestRepository extends JpaRepository<ManualAttendanceRequestEntity, Long> {

    List<ManualAttendanceRequestEntity> findByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    List<ManualAttendanceRequestEntity> findByUserIdAndAttendanceDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<ManualAttendanceRequestEntity> findByUserIdOrderByAttendanceDateDesc(Long userId);

    List<ManualAttendanceRequestEntity> findByManagerIdAndManagerStatus(Long managerId, String managerStatus);

    List<ManualAttendanceRequestEntity> findByHodIdAndManagerStatusAndHodStatus(Long hodId, String managerStatus, String hodStatus);

    List<ManualAttendanceRequestEntity> findByHodIdAndHodStatus(Long hodId, String hodStatus);

}
