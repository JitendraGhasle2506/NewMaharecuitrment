package com.maharecruitment.gov.in.recruitment.repository;

import java.util.Collection;
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
            "subDepartment",
            "designation",
            "preOnboarding",
            "preOnboarding.previousEmployments",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    @Query("select employee from EmployeeEntity employee "
            + "where upper(trim(employee.email)) = upper(trim(:email)) "
            + "order by case when employee.preOnboarding is not null then 0 else 1 end, "
            + "case when upper(coalesce(employee.status, '')) = 'ACTIVE' then 0 else 1 end, "
            + "coalesce(employee.onboardingDate, employee.joiningDate) desc, "
            + "employee.employeeId desc")
    List<EmployeeEntity> findDetailedProfilesByEmail(@Param("email") String email);

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
    @Query("select employee from EmployeeEntity employee where employee.employeeId in :employeeIds")
    List<EmployeeEntity> findDetailedByEmployeeIdIn(@Param("employeeIds") Collection<Long> employeeIds);

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "subDepartment",
            "designation",
            "preOnboarding",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    List<EmployeeEntity> findByStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(String status);

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
    List<EmployeeEntity> findByAgencyAgencyIdAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
            Long agencyId,
            String status);

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
    @Query("select employee from EmployeeEntity employee "
            + "where employee.agency.agencyId = :agencyId "
            + "and upper(trim(coalesce(employee.recruitmentType, ''))) = :recruitmentType "
            + "and upper(trim(coalesce(employee.status, ''))) = :status "
            + "order by lower(employee.fullName), employee.employeeId")
    List<EmployeeEntity> findWorkOrderCandidatesByAgencyRecruitmentTypeAndStatus(
            @Param("agencyId") Long agencyId,
            @Param("recruitmentType") String recruitmentType,
            @Param("status") String status);

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
            + "where upper(trim(coalesce(employee.status, ''))) = :status "
            + "and preOnboarding is not null "
            + "and preOnboarding.onboardedAt is not null "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
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
                    + "where upper(trim(coalesce(employee.status, ''))) = :status "
                    + "and preOnboarding is not null "
                    + "and preOnboarding.onboardedAt is not null "
                    + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
                    + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
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

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "where trim(coalesce(employee.aadhaarNumber, '')) = :aadhaarNumber")
    boolean existsByNormalizedAadhaarNumber(@Param("aadhaarNumber") String aadhaarNumber);

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "where upper(trim(coalesce(employee.panNumber, ''))) = upper(trim(:panNumber))")
    boolean existsByNormalizedPanNumber(@Param("panNumber") String panNumber);

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "where lower(trim(coalesce(employee.email, ''))) = lower(trim(:email))")
    boolean existsByNormalizedEmail(@Param("email") String email);

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "where trim(coalesce(employee.mobile, '')) = :mobile")
    boolean existsByNormalizedMobile(@Param("mobile") String mobile);

    boolean existsByPreOnboardingInterviewDetailRecruitmentInterviewDetailIdAndStatusIgnoreCase(
            Long recruitmentInterviewDetailId, String status);

    long countByPreOnboardingInterviewDetailDesignationVacancyRecruitmentDesignationVacancyIdAndStatusIgnoreCase(
            Long recruitmentDesignationVacancyId,
            String status);

    long countByRecruitmentType(String recruitmentType);

    long countByOnboardingDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

    long countByAgencyAgencyId(Long agencyId);

    List<EmployeeEntity> findByDepartmentRegistration_DepartmentRegistrationId(Long id);

    List<EmployeeEntity> findByDepartmentRegistration_DepartmentRegistrationIdAndRecruitmentType(Long id,
            String recruitmentType);

    List<EmployeeEntity> findByDesignation_DesignationNameIgnoreCaseAndStatusIgnoreCase(String designationName, String status);

    @Query("select employee from EmployeeEntity employee "
            + "where upper(trim(coalesce(employee.recruitmentType, ''))) = 'INTERNAL' "
            + "and trim(coalesce(employee.aadhaarNumber, '')) <> '' "
            + "order by employee.employeeId")
    List<EmployeeEntity> findInternalAttendanceSyncCandidates();

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "subDepartment",
            "designation",
            "preOnboarding",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    @Query("select employee from EmployeeEntity employee "
            + "where upper(trim(coalesce(employee.recruitmentType, ''))) = 'INTERNAL' "
            + "and (:agencyId is null or employee.agency.agencyId = :agencyId) "
            + "and (:departmentRegistrationId is null "
            + "or employee.departmentRegistration.departmentRegistrationId = :departmentRegistrationId) "
            + "and (:subDepartmentId is null or employee.subDepartment.subDeptId = :subDepartmentId) "
            + "and (:employeeStatus is null or upper(trim(coalesce(employee.status, ''))) = :employeeStatus) "
            + "order by lower(coalesce(employee.fullName, '')), employee.employeeId")
    List<EmployeeEntity> findDetailedInternalEmployeesForAttendanceReport(
            @Param("agencyId") Long agencyId,
            @Param("departmentRegistrationId") Long departmentRegistrationId,
            @Param("subDepartmentId") Long subDepartmentId,
            @Param("employeeStatus") String employeeStatus);

    @Query("select count(employee) from EmployeeEntity employee "
            + "where upper(trim(coalesce(employee.recruitmentType, ''))) = 'INTERNAL' "
            + "and trim(coalesce(employee.aadhaarNumber, '')) <> ''")
    long countInternalAttendanceSyncCandidates();

}
