package com.maharecruitment.gov.in.department.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;

@Repository
public interface DepartmentProjectApplicationRepository extends JpaRepository<DepartmentProjectApplicationEntity, Long> {

    boolean existsByRequestId(String requestId);

    List<DepartmentProjectApplicationEntity> findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(
            Long departmentRegistrationId);

    Optional<DepartmentProjectApplicationEntity> findByDepartmentProjectApplicationIdAndDepartmentRegistrationId(
            Long departmentProjectApplicationId,
            Long departmentRegistrationId);
}
