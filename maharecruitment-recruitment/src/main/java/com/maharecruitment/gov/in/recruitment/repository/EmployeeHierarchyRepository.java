package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;

/** Read-only, minimal projections: no salary, contact or identity-document data is loaded. */
public interface EmployeeHierarchyRepository extends Repository<EmployeeEntity, Long> {
    String ACTIVE_EMPLOYEE = """
            upper(e.status) = 'ACTIVE'
            and (u.id is null or u.active = true)
            and not exists (
                select r.relievingId from EmployeeRelievingEntity r
                where r.employee = e and upper(coalesce(r.status, 'INITIATED')) not in ('CANCELLED', 'REJECTED')
                and (upper(r.status) in ('COMPLETED', 'RELIEVED') or r.exitDate <= current_date)
            )
            """;

    @Query("""
            select e.employeeId as employeeId, u.id as userId, e.fullName as employeeName,
                   e.employeeCode as employeeCode, d.departmentId as departmentId,
                   d.departmentName as department, des.designationId as designationId,
                   des.designationName as designation,
                   case when exists (select hu.id from User hu join hu.roles role
                       where hu.id = u.id and upper(role.name) = 'ROLE_HOD')
                       or exists (select m.mappingId from EmployeeReportingMappingEntity m where m.hodUserId = u.id)
                       then true else false end as hod,
                   case when e.photoPath is not null or pre.photoFilePath is not null
                       or exists (select p.id from EmployeeProfile p where p.employee = e and p.photoPath is not null)
                       then true else false end as hasPhoto
            from EmployeeEntity e left join e.user u left join e.department d
            left join e.designation des left join e.preOnboarding pre
            where
            """ + ACTIVE_EMPLOYEE + " order by lower(e.fullName), e.employeeId")
    List<EmployeeRow> findActiveEmployees();

    @Query("""
            select m.mappingId as mappingId, m.employeeId as employeeId,
                   m.managerEmployeeId as managerEmployeeId, m.hodUserId as hodUserId
            from EmployeeReportingMappingEntity m
            where coalesce(m.reportingType, 'PRIMARY') = :reportingType
            order by m.mappingId desc
            """)
    List<ReportingRow> findReportingRelationships(@Param("reportingType") String reportingType);

    @Query("""
            select p.photoPath as profilePhoto, e.photoPath as employeePhoto, pre.photoFilePath as onboardingPhoto
            from EmployeeEntity e left join e.user u left join e.preOnboarding pre
            left join EmployeeProfile p on p.employee = e
            where e.employeeId = :employeeId and
            """ + ACTIVE_EMPLOYEE + " order by p.id desc")
    List<PhotoRow> findPhotoSources(@Param("employeeId") Long employeeId);

    interface EmployeeRow {
        Long getEmployeeId();
        Long getUserId();
        String getEmployeeName();
        String getEmployeeCode();
        Long getDepartmentId();
        String getDepartment();
        Long getDesignationId();
        String getDesignation();
        Boolean getHod();
        Boolean getHasPhoto();
    }

    interface ReportingRow {
        Long getMappingId();
        Long getEmployeeId();
        Long getManagerEmployeeId();
        Long getHodUserId();
    }

    interface PhotoRow {
        String getProfilePhoto();
        String getEmployeePhoto();
        String getOnboardingPhoto();
    }
}
