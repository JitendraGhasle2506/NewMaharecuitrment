package com.maharecruitment.gov.in.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;

@Repository
public interface DepartmentRegistrationRepository extends JpaRepository<DepartmentRegistrationEntity, Long> {

    boolean existsByGstNoIgnoreCase(String gstNo);

    boolean existsByPanNoIgnoreCase(String panNo);

    boolean existsByTanNoIgnoreCase(String tanNo);
}
