package com.maharecruitment.gov.in.recruitment.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    Optional<EmployeeEntity> findByEmployeeCode(String employeeCode);
    Optional<EmployeeEntity> findByEmail(String email);

    Page<EmployeeEntity> findByRecruitmentType(String recruitmentType, Pageable pageable);
}
