package com.maharecruitment.gov.in.attendance.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.attendance.entity.AttendanceLockEntity;



@Repository
public interface AttendanceLockRepository extends JpaRepository<AttendanceLockEntity, Long> {

    Optional<AttendanceLockEntity> findByDeptRegIdAndMonthAndYear(Long deptRegId, Integer month, Integer year);
}
