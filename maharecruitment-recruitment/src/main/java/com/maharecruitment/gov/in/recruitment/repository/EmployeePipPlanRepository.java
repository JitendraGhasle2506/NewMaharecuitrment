package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.maharecruitment.gov.in.recruitment.entity.EmployeePipPlan;
import jakarta.persistence.LockModeType;

public interface EmployeePipPlanRepository extends JpaRepository<EmployeePipPlan, Long> {
    boolean existsByEmployeeIdAndStatus(Long employeeId, EmployeePipPlan.Status status);
    List<EmployeePipPlan> findByEmployeeIdInOrderByCreatedDateTimeDescIdDesc(Collection<Long> employeeIds);
    org.springframework.data.domain.Page<EmployeePipPlan> findByEmployeeIdInOrderByCreatedDateTimeDescIdDesc(
            Collection<Long> employeeIds, org.springframework.data.domain.Pageable pageable);
    @Query("select p.employeeId from EmployeePipPlan p where p.employeeId in :ids and p.status = :status")
    List<Long> findEmployeeIdsByStatus(@Param("ids") Collection<Long> ids, @Param("status") EmployeePipPlan.Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from EmployeePipPlan p where p.id = :id")
    Optional<EmployeePipPlan> lockById(@Param("id") Long id);
}
