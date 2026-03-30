package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    Optional<EmployeeEntity> findByEmployeeCode(String employeeCode);

    Optional<EmployeeEntity> findByEmail(String email);

    Page<EmployeeEntity> findByRecruitmentType(String recruitmentType, Pageable pageable);

    Page<EmployeeEntity> findByStatus(String status, Pageable pageable);

    Page<EmployeeEntity> findByRecruitmentTypeAndStatus(String recruitmentType, String status, Pageable pageable);

    List<EmployeeEntity> findByAgencyAgencyIdOrderByOnboardingDateDescEmployeeIdDesc(Long agencyId);

    List<EmployeeEntity> findByAgencyAgencyIdAndStatusOrderByOnboardingDateDescEmployeeIdDesc(Long agencyId,
            String status);

    Optional<EmployeeEntity> findByEmployeeIdAndAgencyAgencyId(Long employeeId, Long agencyId);

    boolean existsByPreOnboardingInterviewDetailRecruitmentInterviewDetailIdAndStatusIgnoreCase(
            Long recruitmentInterviewDetailId, String status);

    long countByPreOnboardingInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndStatusIgnoreCase(
            Long recruitmentDesignationVacancyId,
            String status);

    List<EmployeeEntity> findByDepartmentRegistration_DepartmentRegistrationId(Long id);

    List<EmployeeEntity> findByDepartmentRegistration_DepartmentRegistrationIdAndRecruitmentType(Long id,
            String recruitmentType);

    List<EmployeeEntity> findByDesignation_DesignationNameIgnoreCaseAndStatusIgnoreCase(String designationName, String status);

}
