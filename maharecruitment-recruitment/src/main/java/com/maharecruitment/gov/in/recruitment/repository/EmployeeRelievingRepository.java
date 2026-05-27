package com.maharecruitment.gov.in.recruitment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeRelievingEntity;

import java.util.Optional;

@Repository
public interface EmployeeRelievingRepository extends JpaRepository<EmployeeRelievingEntity, Long> {
    Optional<EmployeeRelievingEntity> findByEmployee_EmployeeId(Long employeeId);
}
