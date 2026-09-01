package com.maharecruitment.gov.in.recruitment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.recruitment.entity.EmployeeProfile;
import com.maharecruitment.gov.in.recruitment.repository.projection.EmployeeAnniversaryProjection;

@Repository
public interface EmployeeProfileRepository extends JpaRepository<EmployeeProfile, Long> {

    @Query("""
            select profile.employee.employeeId as employeeId,
                   profile.employee.fullName as fullName,
                   profile.marriageDate as marriageDate
            from EmployeeProfile profile
            where upper(trim(coalesce(profile.employee.status, ''))) = 'ACTIVE'
              and profile.marriageDate is not null
            order by lower(profile.employee.fullName), profile.employee.employeeId
            """)
    List<EmployeeAnniversaryProjection> findActiveEmployeeAnniversaries();

    Optional<EmployeeProfile> findByEmployeeEmployeeId(Long employeeId);
}
