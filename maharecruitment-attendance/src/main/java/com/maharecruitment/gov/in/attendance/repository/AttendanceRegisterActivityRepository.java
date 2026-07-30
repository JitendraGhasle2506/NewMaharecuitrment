package com.maharecruitment.gov.in.attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.AttendanceRegisterActivityEntity;

@Repository
public interface AttendanceRegisterActivityRepository extends JpaRepository<AttendanceRegisterActivityEntity, Long> {
    
    List<AttendanceRegisterActivityEntity> findByEmployeeUserIdAndMonthAndYearOrderByActionTimestampDesc(Long employeeUserId, Integer month, Integer year);
    
}
