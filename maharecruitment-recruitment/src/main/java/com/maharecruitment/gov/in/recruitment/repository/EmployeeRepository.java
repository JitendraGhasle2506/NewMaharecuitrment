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
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeAgencyFilterProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeBirthdayProjection;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeListProjection;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {

    @Query("""
            select employee.employeeId as employeeId,
                   employee.fullName as fullName,
                   employee.dateOfBirth as dateOfBirth
            from EmployeeEntity employee
            where upper(trim(coalesce(employee.status, ''))) = 'ACTIVE'
              and employee.dateOfBirth is not null
            order by lower(employee.fullName), employee.employeeId
            """)
    List<EmployeeBirthdayProjection> findActiveEmployeeBirthdays();

    Optional<EmployeeEntity> findByEmployeeCode(String employeeCode);

    Optional<EmployeeEntity> findByEmployeeCodeIgnoreCase(String employeeCode);

    Optional<EmployeeEntity> findByUser_Id(Long userId);

    Optional<EmployeeEntity> findByUser_IdAndStatusIgnoreCase(Long userId, String status);

    @EntityGraph(attributePaths = { "designation", "subDepartment" })
    List<EmployeeEntity> findByEmployeeIdInOrderByFullNameAscEmployeeIdAsc(Collection<Long> employeeIds);

    @EntityGraph(attributePaths = "user")
    @Query("select employee from EmployeeEntity employee "
            + "where employee.preOnboarding.preOnboardingId = :preOnboardingId")
    Optional<EmployeeEntity> findByPreOnboardingId(@Param("preOnboardingId") Long preOnboardingId);

    @EntityGraph(attributePaths = { "designation", "user", "user.roles" })
    @Query("""
            select employee
            from EmployeeEntity employee
            join employee.designation designation
            where upper(trim(coalesce(employee.status, ''))) = :status
              and upper(trim(coalesce(designation.activeFlag, ''))) = :activeFlag
            order by lower(designation.designationName), lower(employee.fullName), employee.employeeId
            """)
    List<EmployeeEntity> findActiveEmployeesForDesignationRoleAssignment(
            @Param("status") String status,
            @Param("activeFlag") String activeFlag);

    @EntityGraph(attributePaths = { "designation", "user", "user.roles" })
    List<EmployeeEntity> findByDesignation_DesignationIdAndStatusIgnoreCaseOrderByFullNameAscEmployeeIdAsc(
            Long designationId,
            String status);

    boolean existsByUser_Id(Long userId);

    boolean existsByEmailIgnoreCaseAndEmployeeIdNot(String email, Long employeeId);

    boolean existsByMobileAndEmployeeIdNot(String mobile, Long employeeId);

    List<EmployeeEntity> findByRecruitmentType(String recruitmentType);
    Page<EmployeeEntity> findByRecruitmentType(String recruitmentType, Pageable pageable);

    Page<EmployeeEntity> findByStatus(String status, Pageable pageable);

    List<EmployeeEntity> findByRecruitmentTypeAndStatus(String recruitmentType, String status);
    Page<EmployeeEntity> findByRecruitmentTypeAndStatus(String recruitmentType, String status, Pageable pageable);

    @EntityGraph(attributePaths = { "designation", "designation.levels" })
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

    @EntityGraph(attributePaths = { "designation", "designation.levels" })
    @Query("select employee from EmployeeEntity employee "
            + "where upper(trim(coalesce(employee.status, ''))) = :status "
            + "order by lower(employee.fullName), employee.employeeId")
    List<EmployeeEntity> findActiveEmployeeOptions(@Param("status") String status);

    @EntityGraph(attributePaths = { "designation", "designation.levels" })
    @Query("select distinct employee from EmployeeEntity employee "
            + "where employee.employeeId in :employeeIds")
    List<EmployeeEntity> findReplacementEmployeesByEmployeeIdIn(
            @Param("employeeIds") Collection<Long> employeeIds);

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

    @Query(value = "select employee.employeeId as employeeId, "
            + "employee.employeeCode as employeeCode, "
            + "employee.fullName as fullName, "
            + "employee.email as email, "
            + "designation.designationName as designation, "
            + "employee.onboardingDate as mahaitJoiningDate, "
            + "employee.recruitmentType as recruitmentType, "
            + "agency.agencyName as agencyName, "
            + "employee.status as status "
            + "from EmployeeEntity employee "
            + "left join employee.agency agency "
            + "left join employee.designation designation "
            + "where upper(trim(coalesce(employee.status, ''))) = :status "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "and (:recruitmentType is null or upper(employee.recruitmentType) = :recruitmentType) "
            + "and (:agencyId is null or agency.agencyId = :agencyId) "
            + "and (:searchPattern is null "
            + "or upper(coalesce(employee.employeeCode, '')) like :searchPattern "
            + "or upper(coalesce(employee.fullName, '')) like :searchPattern "
            + "or upper(coalesce(employee.email, '')) like :searchPattern)",
            countQuery = "select count(employee) "
                    + "from EmployeeEntity employee "
                    + "where upper(trim(coalesce(employee.status, ''))) = :status "
                    + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
                    + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
                    + "and (:recruitmentType is null or upper(employee.recruitmentType) = :recruitmentType) "
                    + "and (:agencyId is null or employee.agency.agencyId = :agencyId) "
                    + "and (:searchPattern is null "
                    + "or upper(coalesce(employee.employeeCode, '')) like :searchPattern "
                    + "or upper(coalesce(employee.fullName, '')) like :searchPattern "
                    + "or upper(coalesce(employee.email, '')) like :searchPattern)")
    Page<EmployeeListProjection> findEmployeeListPageByStatusAndFilters(
            @Param("status") String status,
            @Param("recruitmentType") String recruitmentType,
            @Param("agencyId") Long agencyId,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("select agency.agencyId as agencyId, agency.agencyName as agencyName "
            + "from EmployeeEntity employee "
            + "join employee.agency agency "
            + "where upper(trim(coalesce(employee.status, ''))) = :status "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%' "
            + "group by agency.agencyId, agency.agencyName "
            + "order by lower(agency.agencyName), agency.agencyId")
    List<EmployeeAgencyFilterProjection> findAgencyFilterOptionsByEmployeeStatus(
            @Param("status") String status);

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
              and not exists (
                   select mapping.employeeCellMappingId
                   from EmployeeCellMappingEntity mapping
                   where mapping.employee = employee
              )
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
                   or upper(coalesce(designation.designationName, '')) like :searchPattern)
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
                      and not exists (
                           select mapping.employeeCellMappingId
                           from EmployeeCellMappingEntity mapping
                           where mapping.employee = employee
                      )
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
                           or upper(coalesce(designation.designationName, '')) like :searchPattern)
                    """)
    Page<EmployeeEntity> findActiveOnboardedWithoutCellMapping(
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

    @Query("select count(employee.employeeId) from EmployeeEntity employee "
            + "where upper(trim(coalesce(employee.status, ''))) = 'ACTIVE' "
            + "and trim(coalesce(employee.employeeCode, '')) <> '' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) <> 'PENDING' "
            + "and upper(trim(coalesce(employee.employeeCode, ''))) not like 'TMP-%'")
    long countActiveAttendanceEmployees();

    long countByOnboardingDateBetween(java.time.LocalDate startDate, java.time.LocalDate endDate);

    @Query("""
            select trim(department.departmentName) as department,
                   count(employee.employeeId) as employeeCount
            from EmployeeEntity employee
            join employee.departmentRegistration department
            where trim(coalesce(department.departmentName, '')) <> ''
            group by trim(department.departmentName)
            """)
    List<EmployeeDepartmentCountProjection> summarizeEmployeeCountsByDepartment();

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
            + "or upper(trim(coalesce(d.roleName, ''))) = upper(trim(:roleName)) "
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
