package com.maharecruitment.gov.in.recruitment.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeTaskLogEntity;

@Repository
public interface EmployeeTaskLogRepository extends JpaRepository<EmployeeTaskLogEntity, Long> {
    
    List<EmployeeTaskLogEntity> findByEmployee_EmployeeIdAndTaskDate(Long employeeId, LocalDate taskDate);

    Page<EmployeeTaskLogEntity> findByEmployee_EmployeeIdOrderByTaskDateDesc(Long employeeId, Pageable pageable);

    @Query("SELECT t FROM EmployeeTaskLogEntity t WHERE t.employee.employeeId = :employeeId AND YEAR(t.taskDate) = :year AND MONTH(t.taskDate) = :month ORDER BY t.taskDate DESC")
    Page<EmployeeTaskLogEntity> findByEmployeeIdAndMonthAndYear(@Param("employeeId") Long employeeId, @Param("month") int month, @Param("year") int year, Pageable pageable);

    List<EmployeeTaskLogEntity> findByEmployee_EmployeeIdInAndStatusOrderByTaskDateAsc(List<Long> employeeIds, String status);
}
