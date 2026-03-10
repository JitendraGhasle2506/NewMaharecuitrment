package com.maharecruitment.gov.in.department.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentProjectCountByDepartmentAndSubDepartmentProjection;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentProjectCountByDepartmentProjection;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentProjectCountBySubDepartmentProjection;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentSubmittedProjectCountProjection;

@Repository
public interface DepartmentProjectApplicationRepository extends JpaRepository<DepartmentProjectApplicationEntity, Long> {

    boolean existsByRequestId(String requestId);

    List<DepartmentProjectApplicationEntity> findByDepartmentRegistrationIdOrderByDepartmentProjectApplicationIdDesc(
            Long departmentRegistrationId);

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
