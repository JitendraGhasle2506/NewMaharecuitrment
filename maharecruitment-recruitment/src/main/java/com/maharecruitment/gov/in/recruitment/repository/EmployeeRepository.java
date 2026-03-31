package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "subDepartment",
            "designation",
            "preOnboarding",
            "preOnboarding.previousEmployments",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    Optional<EmployeeEntity> findDetailedByEmployeeId(Long employeeId);

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "designation",
            "preOnboarding",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    @Query(value = "select employee "
            + "from EmployeeEntity employee "
            + "left join employee.preOnboarding preOnboarding "
            + "left join preOnboarding.interviewDetail interviewDetail "
            + "left join interviewDetail.recruitmentNotification notification "
            + "left join notification.projectMst project "
            + "where upper(employee.status) = :status "
            + "and (:recruitmentType is null or upper(employee.recruitmentType) = :recruitmentType) "
            + "and (:searchPattern is null "
            + "or upper(coalesce(employee.requestId, '')) like :searchPattern "
            + "or upper(coalesce(project.projectName, '')) like :searchPattern "
            + "or upper(coalesce(employee.recruitmentType, '')) like :searchPattern)",
            countQuery = "select count(employee) "
                    + "from EmployeeEntity employee "
                    + "left join employee.preOnboarding preOnboarding "
                    + "left join preOnboarding.interviewDetail interviewDetail "
                    + "left join interviewDetail.recruitmentNotification notification "
                    + "left join notification.projectMst project "
                    + "where upper(employee.status) = :status "
                    + "and (:recruitmentType is null or upper(employee.recruitmentType) = :recruitmentType) "
                    + "and (:searchPattern is null "
                    + "or upper(coalesce(employee.requestId, '')) like :searchPattern "
                    + "or upper(coalesce(project.projectName, '')) like :searchPattern "
                    + "or upper(coalesce(employee.recruitmentType, '')) like :searchPattern)")
    Page<EmployeeEntity> findPageByStatusAndFilters(
            @Param("status") String status,
            @Param("recruitmentType") String recruitmentType,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    List<EmployeeEntity> findByAgencyAgencyIdOrderByOnboardingDateDescEmployeeIdDesc(Long agencyId);

    List<EmployeeEntity> findByAgencyAgencyIdAndStatusOrderByOnboardingDateDescEmployeeIdDesc(Long agencyId,
            String status);

    Optional<EmployeeEntity> findByEmployeeIdAndAgencyAgencyId(Long employeeId, Long agencyId);

    boolean existsByPreOnboardingInterviewDetailRecruitmentInterviewDetailIdAndStatusIgnoreCase(
            Long recruitmentInterviewDetailId, String status);

    long countByPreOnboardingInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndStatusIgnoreCase(
            Long recruitmentDesignationVacancyId,
            String status);

    long countByRecruitmentType(String recruitmentType);

    long countByOnboardingDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

    List<EmployeeEntity> findByDepartmentRegistration_DepartmentRegistrationId(Long id);

    List<EmployeeEntity> findByDepartmentRegistration_DepartmentRegistrationIdAndRecruitmentType(Long id,
            String recruitmentType);

    List<EmployeeEntity> findByDesignation_DesignationNameIgnoreCaseAndStatusIgnoreCase(String designationName, String status);

}
