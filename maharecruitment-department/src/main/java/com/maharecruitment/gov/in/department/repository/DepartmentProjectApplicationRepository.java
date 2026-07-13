package com.maharecruitment.gov.in.department.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.projection.CellEmployeeCountProjection;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentProjectCountByDepartmentAndSubDepartmentProjection;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentProjectCountByDepartmentProjection;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentProjectCountBySubDepartmentProjection;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentSubmittedProjectCountProjection;

@Repository
public interface DepartmentProjectApplicationRepository extends JpaRepository<DepartmentProjectApplicationEntity, Long> {

    boolean existsByRequestId(String requestId);

    long countByApplicationStatus(DepartmentApplicationStatus applicationStatus);

    @Query("""
            select
                case
                    when trim(coalesce(cell.cellName, '')) = '' then :unassignedCell
                    else trim(cell.cellName)
                end as cellName,
                coalesce(sum(case when upper(trim(coalesce(employee.recruitmentType, ''))) = :internalType then 1 else 0 end), 0) as internalEmployees,
                coalesce(sum(case when upper(trim(coalesce(employee.recruitmentType, ''))) = :externalType then 1 else 0 end), 0) as externalEmployees
            from ProjectMst project
            left join project.cell cell,
                 DepartmentProjectApplicationEntity application,
                 EmployeeEntity employee
            where application.departmentProjectApplicationId = project.applicationId
              and employee.requestId = application.requestId
              and trim(coalesce(employee.requestId, '')) <> ''
            group by
                case
                    when trim(coalesce(cell.cellName, '')) = '' then :unassignedCell
                    else trim(cell.cellName)
                end
            """)
    List<CellEmployeeCountProjection> summarizeEmployeeCountsByProjectCell(
            @Param("internalType") String internalType,
            @Param("externalType") String externalType,
            @Param("unassignedCell") String unassignedCell);

    Optional<DepartmentProjectApplicationEntity> findByRequestIdIgnoreCase(String requestId);
    
    List<DepartmentProjectApplicationEntity> findByApplicationStatusInOrderByDepartmentProjectApplicationIdDesc(
            Collection<DepartmentApplicationStatus> applicationStatuses);

    Page<DepartmentProjectApplicationEntity> findByApplicationStatusInOrderByDepartmentProjectApplicationIdDesc(
            Collection<DepartmentApplicationStatus> applicationStatuses,
            Pageable pageable);


    List<DepartmentProjectApplicationEntity> findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(
            Long departmentRegistrationId);

    Page<DepartmentProjectApplicationEntity> findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(
            Long departmentRegistrationId,
            Pageable pageable);

    Optional<DepartmentProjectApplicationEntity> findByDepartmentProjectApplicationIdAndDepartmentRegistrationId(
            Long departmentProjectApplicationId,
            Long departmentRegistrationId);

    List<DepartmentProjectApplicationEntity> findByDepartmentRegistrationIdAndApplicationStatusInOrderByDepartmentProjectApplicationIdDesc(
            Long departmentRegistrationId,
            Collection<DepartmentApplicationStatus> applicationStatuses);

    List<DepartmentProjectApplicationEntity> findByDepartmentRegistrationIdInAndApplicationStatusInOrderByDepartmentProjectApplicationIdDesc(
            Collection<Long> departmentRegistrationIds,
            Collection<DepartmentApplicationStatus> applicationStatuses);

    List<DepartmentProjectApplicationEntity> findByDepartmentIdAndSubDepartmentIdAndApplicationStatusInOrderByDepartmentProjectApplicationIdDesc(
            Long departmentId,
            Long subDepartmentId,
            Collection<DepartmentApplicationStatus> applicationStatuses);

    @Query(
            "select a.departmentId as departmentId, "
                    + "count(a.departmentProjectApplicationId) as projectCount "
                    + "from DepartmentProjectApplicationEntity a "
                    + "where a.applicationStatus in :applicationStatuses "
                    + "group by a.departmentId")
    List<DepartmentProjectCountByDepartmentProjection> countProjectApplicationsByDepartment(
            @Param("applicationStatuses") Collection<DepartmentApplicationStatus> applicationStatuses);

    @Query(
            "select a.subDepartmentId as subDepartmentId, "
                    + "count(a.departmentProjectApplicationId) as projectCount "
                    + "from DepartmentProjectApplicationEntity a "
                    + "where a.departmentId = :departmentId "
                    + "and a.applicationStatus in :applicationStatuses "
                    + "group by a.subDepartmentId")
    List<DepartmentProjectCountBySubDepartmentProjection> countProjectApplicationsBySubDepartment(
            @Param("departmentId") Long departmentId,
            @Param("applicationStatuses") Collection<DepartmentApplicationStatus> applicationStatuses);

    @Query(
            "select a.departmentId as departmentId, "
                    + "a.subDepartmentId as subDepartmentId, "
                    + "count(a.departmentProjectApplicationId) as projectCount "
                    + "from DepartmentProjectApplicationEntity a "
                    + "where a.applicationStatus in :applicationStatuses "
                    + "group by a.departmentId, a.subDepartmentId")
    List<DepartmentProjectCountByDepartmentAndSubDepartmentProjection> countProjectApplicationsByDepartmentAndSubDepartment(
            @Param("applicationStatuses") Collection<DepartmentApplicationStatus> applicationStatuses);

    @Query(
            "select a.departmentRegistrationId as departmentRegistrationId, "
                    + "count(a.departmentProjectApplicationId) as projectCount "
                    + "from DepartmentProjectApplicationEntity a "
                    + "where a.applicationStatus in :applicationStatuses "
                    + "group by a.departmentRegistrationId")
    List<DepartmentSubmittedProjectCountProjection> countSubmittedProjectsByDepartmentRegistration(
            @Param("applicationStatuses") Collection<DepartmentApplicationStatus> applicationStatuses);
}
