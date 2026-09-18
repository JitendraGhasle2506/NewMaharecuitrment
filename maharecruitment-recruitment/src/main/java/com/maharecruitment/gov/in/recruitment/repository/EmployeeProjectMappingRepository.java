package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;

@Repository
public interface EmployeeProjectMappingRepository extends JpaRepository<EmployeeProjectMappingEntity, Long> {

    @EntityGraph(attributePaths = { "employee", "project" })
    Optional<EmployeeProjectMappingEntity> findByEmployeeEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = { "employee", "project" })
    List<EmployeeProjectMappingEntity> findByEmployeeEmployeeIdIn(Collection<Long> employeeIds);
}
