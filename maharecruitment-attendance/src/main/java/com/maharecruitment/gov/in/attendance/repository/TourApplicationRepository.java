package com.maharecruitment.gov.in.attendance.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.maharecruitment.gov.in.attendance.entity.TourApplicationEntity;

@Repository
public interface TourApplicationRepository extends JpaRepository<TourApplicationEntity, Long> {
    List<TourApplicationEntity> findByEmployeeIdOrderByApplicationDateDesc(Long employeeId);
    List<TourApplicationEntity> findByEmployeeIdInAndStatusOrderByApplicationDateDesc(List<Long> employeeIds, String status);
    
    List<TourApplicationEntity> findByEmployeeIdInAndStatusInOrderByApplicationDateDesc(List<Long> employeeIds, List<String> statuses);
    List<TourApplicationEntity> findByEmployeeIdAndStatus(Long employeeId, String status);
}
