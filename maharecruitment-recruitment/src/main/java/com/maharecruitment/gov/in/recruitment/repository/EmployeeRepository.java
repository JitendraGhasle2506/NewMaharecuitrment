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

    Optional<EmployeeEntity> findByEmployeeCodeIgnoreCase(String employeeCode);

    Optional<EmployeeEntity> findByUser_Id(Long userId);

    Optional<EmployeeEntity> findByUser_IdAndStatusIgnoreCase(Long userId, String status);

    boolean existsByUser_Id(Long userId);

    boolean existsByEmailIgnoreCaseAndEmployeeIdNot(String email, Long employeeId);

    boolean existsByMobileAndEmployeeIdNot(String mobile, Long employeeId);

    List<EmployeeEntity> findByRecruitmentType(String recruitmentType);
    Page<EmployeeEntity> findByRecruitmentType(String recruitmentType, Pageable pageable);

    Page<EmployeeEntity> findByStatus(String status, Pageable pageable);

    List<EmployeeEntity> findByRecruitmentTypeAndStatus(String recruitmentType, String status);
    Page<EmployeeEntity> findByRecruitmentTypeAndStatus(String recruitmentType, String status, Pageable pageable);

    List<EmployeeEntity> findByRecruitmentTypeIgnoreCaseAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
            String recruitmentType, String status);

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "department",
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
            "department",
            "subDepartment",
            "designation",
            "preOnboarding",
            "preOnboarding.previousEmployments",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    @Query("select employee from EmployeeEntity employee "
            + "where employee.user.id = :userId")
    Optional<EmployeeEntity> findDetailedByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {
            "departmentRegistration",
            "department",
            "subDepartment",
            "subDepartment.department",
            "designation",
            "preOnboarding" })
    @Query("select employee from EmployeeEntity employee "
            + "where employee.user.id = :userId")
    Optional<EmployeeEntity> findMobileLoginProfileByUserId(@Param("userId") Long userId);

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "department",
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

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "designation",
            "preOnboarding",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    @Query(value = """
            select employee
            from EmployeeEntity employee
            left join employee.departmentRegistration department
            left join employee.designation designation
            left join employee.preOnboarding preOnboarding
            left join preOnboarding.interviewDetail interviewDetail
            left join interviewDetail.recruitmentNotification notification
            left join notification.projectMst project
            where upper(trim(coalesce(employee.status, ''))) = 'ACTIVE'
              and trim(coalesce(employee.employeeCode, '')) <> ''
              and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING'
              and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%'
              and (:recruitmentType is null
                   or upper(trim(coalesce(employee.recruitmentType, ''))) = :recruitmentType)
              and (:searchPattern is null
                   or upper(coalesce(employee.fullName, '')) like :searchPattern
                   or upper(coalesce(employee.employeeCode, '')) like :searchPattern
                   or upper(coalesce(employee.email, '')) like :searchPattern
                   or upper(coalesce(employee.mobile, '')) like :searchPattern
                   or upper(coalesce(employee.requestId, '')) like :searchPattern
                   or upper(coalesce(project.projectName, '')) like :searchPattern
                   or upper(coalesce(department.departmentName, '')) like :searchPattern
                   or upper(coalesce(designation.designationName, '')) like :searchPattern
                   or exists (
                        select mapping.employeeLocationMappingId
                        from EmployeeLocationMappingEntity mapping
                        join mapping.location location
                        where mapping.employee = employee
                          and (
                                upper(coalesce(location.locationName, '')) like :searchPattern
                             or upper(coalesce(location.officeName, '')) like :searchPattern
                          )
                   ))
            """,
            countQuery = """
                    select count(employee)
                    from EmployeeEntity employee
                    left join employee.departmentRegistration department
                    left join employee.designation designation
                    left join employee.preOnboarding preOnboarding
                    left join preOnboarding.interviewDetail interviewDetail
                    left join interviewDetail.recruitmentNotification notification
                    left join notification.projectMst project
                    where upper(trim(coalesce(employee.status, ''))) = 'ACTIVE'
                      and trim(coalesce(employee.employeeCode, '')) <> ''
                      and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING'
                      and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%'
                      and (:recruitmentType is null
                           or upper(trim(coalesce(employee.recruitmentType, ''))) = :recruitmentType)
                      and (:searchPattern is null
                           or upper(coalesce(employee.fullName, '')) like :searchPattern
                           or upper(coalesce(employee.employeeCode, '')) like :searchPattern
                           or upper(coalesce(employee.email, '')) like :searchPattern
                           or upper(coalesce(employee.mobile, '')) like :searchPattern
                           or upper(coalesce(employee.requestId, '')) like :searchPattern
                           or upper(coalesce(project.projectName, '')) like :searchPattern
                           or upper(coalesce(department.departmentName, '')) like :searchPattern
                           or upper(coalesce(designation.designationName, '')) like :searchPattern
                           or exists (
                                select mapping.employeeLocationMappingId
                                from EmployeeLocationMappingEntity mapping
                                join mapping.location location
                                where mapping.employee = employee
                                  and (
                                        upper(coalesce(location.locationName, '')) like :searchPattern
                                     or upper(coalesce(location.officeName, '')) like :searchPattern
                                  )
                           ))
                    """)
    Page<EmployeeEntity> findActiveOnboardedForLocationMapping(
            @Param("recruitmentType") String recruitmentType,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    List<EmployeeEntity> findByAgencyAgencyIdOrderByOnboardingDateDescEmployeeIdDesc(Long agencyId);

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "subDepartment",
            "designation",
            "preOnboarding",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    @Query(value = "select employee "
            + "from EmployeeEntity employee "
            + "join employee.preOnboarding preOnboarding "
            + "where employee.agency.agencyId = :agencyId "
            + "and upper(trim(coalesce(employee.status, ''))) in :statuses "
            + "and preOnboarding.onboardedAt is not null "
            + "and trim(coalesce(employee.employeeCode, '')) <> '' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "order by employee.onboardingDate desc, employee.employeeId desc",
            countQuery = "select count(employee) "
                    + "from EmployeeEntity employee "
                    + "join employee.preOnboarding preOnboarding "
                    + "where employee.agency.agencyId = :agencyId "
                    + "and upper(trim(coalesce(employee.status, ''))) in :statuses "
                    + "and preOnboarding.onboardedAt is not null "
                    + "and trim(coalesce(employee.employeeCode, '')) <> '' "
                    + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
                    + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' ")
    Page<EmployeeEntity> findPageByAgencyAgencyIdAndStatuses(
            @Param("agencyId") Long agencyId,
            @Param("statuses") Collection<String> statuses,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "agency",
            "departmentRegistration",
            "subDepartment",
            "designation",
            "preOnboarding",
            "preOnboarding.interviewDetail",
            "preOnboarding.interviewDetail.recruitmentNotification",
            "preOnboarding.interviewDetail.recruitmentNotification.projectMst" })
    @Query("select employee "
            + "from EmployeeEntity employee "
            + "join employee.preOnboarding preOnboarding "
            + "where employee.agency.agencyId = :agencyId "
            + "and upper(trim(coalesce(employee.status, ''))) = :status "
            + "and preOnboarding.onboardedAt is not null "
            + "and trim(coalesce(employee.employeeCode, '')) <> '' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "order by employee.onboardingDate desc, employee.employeeId desc")
    List<EmployeeEntity> findByAgencyAgencyIdAndStatusOrderByOnboardingDateDescEmployeeIdDesc(
            @Param("agencyId") Long agencyId,
            @Param("status") String status);

    @Query(value = "select e from EmployeeEntity e "
            + "left join e.preOnboarding pre "
            + "left join pre.interviewDetail interview "
            + "left join interview.recruitmentNotification notification "
            + "left join notification.projectMst project "
            + "where e.agency.agencyId = :agencyId "
            + "and upper(e.status) = :status "
            + "and (:search is null or "
            + "upper(e.fullName) like :search or "
            + "upper(e.employeeCode) like :search or "
            + "upper(e.email) like :search or "
            + "upper(project.projectName) like :search or "
            + "upper(e.requestId) like :search)",
            countQuery = "select count(e) from EmployeeEntity e "
            + "left join e.preOnboarding pre "
            + "left join pre.interviewDetail interview "
            + "left join interview.recruitmentNotification notification "
            + "left join notification.projectMst project "
            + "where e.agency.agencyId = :agencyId "
            + "and upper(e.status) = :status "
            + "and (:search is null or "
            + "upper(e.fullName) like :search or "
            + "upper(e.employeeCode) like :search or "
            + "upper(e.email) like :search or "
            + "upper(project.projectName) like :search or "
            + "upper(e.requestId) like :search)")
    Page<EmployeeEntity> findByAgencyAndStatusWithSearch(
            @Param("agencyId") Long agencyId,
            @Param("status") String status,
            @Param("search") String search,
            Pageable pageable);

    Optional<EmployeeEntity> findByEmployeeIdAndAgencyAgencyId(Long employeeId, Long agencyId);

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "left join employee.preOnboarding preOnboarding "
            + "where trim(coalesce(employee.aadhaarNumber, '')) = :aadhaarNumber "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "and (preOnboarding is null or preOnboarding.onboardedAt is not null) "
            + "and (:excludePreOnboardingId is null or preOnboarding is null "
            + "or preOnboarding.preOnboardingId <> :excludePreOnboardingId)")
    boolean existsByNormalizedAadhaarNumberExcludingPreOnboardingId(
            @Param("aadhaarNumber") String aadhaarNumber,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "left join employee.preOnboarding preOnboarding "
            + "where upper(trim(coalesce(employee.panNumber, ''))) = upper(trim(:panNumber)) "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "and (preOnboarding is null or preOnboarding.onboardedAt is not null) "
            + "and (:excludePreOnboardingId is null or preOnboarding is null "
            + "or preOnboarding.preOnboardingId <> :excludePreOnboardingId)")
    boolean existsByNormalizedPanNumberExcludingPreOnboardingId(
            @Param("panNumber") String panNumber,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "left join employee.preOnboarding preOnboarding "
            + "where lower(trim(coalesce(employee.email, ''))) = lower(trim(:email)) "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "and (preOnboarding is null or preOnboarding.onboardedAt is not null) "
            + "and (:excludePreOnboardingId is null or preOnboarding is null "
            + "or preOnboarding.preOnboardingId <> :excludePreOnboardingId)")
    boolean existsByNormalizedEmailExcludingPreOnboardingId(
            @Param("email") String email,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

    @Query("select case when count(employee) > 0 then true else false end "
            + "from EmployeeEntity employee "
            + "left join employee.preOnboarding preOnboarding "
            + "where trim(coalesce(employee.mobile, '')) = :mobile "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "and (preOnboarding is null or preOnboarding.onboardedAt is not null) "
            + "and (:excludePreOnboardingId is null or preOnboarding is null "
            + "or preOnboarding.preOnboardingId <> :excludePreOnboardingId)")
    boolean existsByNormalizedMobileExcludingPreOnboardingId(
            @Param("mobile") String mobile,
            @Param("excludePreOnboardingId") Long excludePreOnboardingId);

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

    @Query("select e from EmployeeEntity e "
            + "left join e.user u "
            + "left join u.roles r "
            + "left join e.designation d "
            + "where upper(trim(coalesce(e.status, ''))) = 'ACTIVE' "
            + "and (upper(trim(coalesce(r.name, ''))) = upper(trim(:roleName)) "
            + "or upper(trim(coalesce(d.designationName, ''))) in :designationNames "
            + "or upper(trim(coalesce(d.designationName, ''))) like :designationNamePattern) "
            + "order by lower(e.fullName), e.employeeId")
    List<EmployeeEntity> findActiveEmployeesByRoleNameOrDesignationNames(
            @Param("roleName") String roleName,
            @Param("designationNames") Collection<String> designationNames,
            @Param("designationNamePattern") String designationNamePattern);

    @Query("select e from EmployeeEntity e "
            + "where upper(trim(coalesce(e.status, ''))) = 'ACTIVE' "
            + "and not exists (select mapping.mappingId from EmployeeReportingMappingEntity mapping "
            + "where mapping.managerEmployeeId = e.employeeId "
            + "and upper(trim(mapping.managerType)) in ('HOD', 'STM', 'PM')) "
            + "order by lower(e.fullName), e.employeeId")
    List<EmployeeEntity> findActiveEmployeesNotMappedAsReportingManagers();

    List<EmployeeEntity> findByFullNameIgnoreCaseAndStatusIgnoreCase(String fullName, String status);

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

    @Query("select e from EmployeeEntity e "
            + "where upper(e.status) = 'ACTIVE' "
            + "and (:search is null or "
            + "     lower(cast(e.fullName as String)) like :search or "
            + "     lower(cast(e.email as String)) like :search or "
            + "     lower(cast(e.mobile as String)) like :search) "
            + "order by e.fullName asc, e.employeeId asc")
    Page<EmployeeEntity> findActiveWithSearch(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = "designation")
    @Query("select e from EmployeeEntity e "
            + "where upper(trim(coalesce(e.status, ''))) = 'ACTIVE' "
            + "and e.designation.designationId = :designationId "
            + "and (:search is null or "
            + "     lower(cast(e.fullName as String)) like :search or "
            + "     lower(cast(e.employeeCode as String)) like :search or "
            + "     lower(cast(e.email as String)) like :search) "
            + "order by e.fullName asc, e.employeeId asc")
    Page<EmployeeEntity> findActiveByDesignationWithSearch(
            @Param("designationId") Long designationId,
            @Param("search") String search,
            Pageable pageable);

    @EntityGraph(attributePaths = "designation")
    @Query("select e from EmployeeEntity e "
            + "where upper(trim(coalesce(e.status, ''))) = 'ACTIVE' "
            + "and e.designation.designationId = :designationId "
            + "and upper(trim(coalesce(e.levelCode, ''))) = upper(trim(:levelCode)) "
            + "and (:search is null or "
            + "     lower(cast(e.fullName as String)) like :search or "
            + "     lower(cast(e.employeeCode as String)) like :search or "
            + "     lower(cast(e.email as String)) like :search) "
            + "order by e.fullName asc, e.employeeId asc")
    Page<EmployeeEntity> findActiveByDesignationAndLevelWithSearch(
            @Param("designationId") Long designationId,
            @Param("levelCode") String levelCode,
            @Param("search") String search,
            Pageable pageable);
}
