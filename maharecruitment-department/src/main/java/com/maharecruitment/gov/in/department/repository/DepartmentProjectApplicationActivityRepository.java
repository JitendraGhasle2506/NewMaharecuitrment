package com.maharecruitment.gov.in.department.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationActivityEntity;

@Repository
public interface DepartmentProjectApplicationActivityRepository
        extends JpaRepository<DepartmentProjectApplicationActivityEntity, Long> {

    List<DepartmentProjectApplicationActivityEntity> findByApplicationDepartmentProjectApplicationIdOrderByActionTimestampDesc(
            Long departmentProjectApplicationId);
}
