package com.maharecruitment.gov.in.recruitment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeRelievingEntity;

import java.util.Optional;

import java.util.List;

@Repository
public interface EmployeeRelievingRepository extends JpaRepository<EmployeeRelievingEntity, Long> {
    List<EmployeeRelievingEntity> findByEmployee_EmployeeId(Long employeeId);
    List<EmployeeRelievingEntity> findByEmployee_Agency_AgencyId(Long agencyId);
    org.springframework.data.domain.Page<EmployeeRelievingEntity> findByEmployee_Agency_AgencyId(Long agencyId, org.springframework.data.domain.Pageable pageable);
    List<EmployeeRelievingEntity> findByEmployee_DepartmentRegistration_DepartmentRegistrationId(Long departmentRegistrationId);
}
