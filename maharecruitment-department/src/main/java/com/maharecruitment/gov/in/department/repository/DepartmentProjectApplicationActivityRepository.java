package com.maharecruitment.gov.in.department.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationActivityEntity;

@Repository
public interface DepartmentProjectApplicationActivityRepository
        extends JpaRepository<DepartmentProjectApplicationActivityEntity, Long> {

    List<DepartmentProjectApplicationActivityEntity> findByApplicationDepartmentProjectApplicationIdOrderByActionTimestampDesc(
            Long departmentProjectApplicationId);

    @Query("""
            select activity
            from DepartmentProjectApplicationActivityEntity activity
            join fetch activity.application application
            where application.departmentProjectApplicationId in :applicationIds
              and activity.newStatus = :newStatus
              and activity.actionTimestamp is not null
            order by application.departmentProjectApplicationId asc, activity.actionTimestamp desc
            """)
    List<DepartmentProjectApplicationActivityEntity> findIssueActivitiesForApplications(
            @Param("applicationIds") Collection<Long> applicationIds,
            @Param("newStatus") DepartmentApplicationStatus newStatus);
}
