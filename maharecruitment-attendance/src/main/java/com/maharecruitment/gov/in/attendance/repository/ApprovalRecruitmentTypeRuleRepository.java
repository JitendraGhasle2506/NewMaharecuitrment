package com.maharecruitment.gov.in.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.maharecruitment.gov.in.attendance.entity.ApprovalRecruitmentTypeRuleEntity;

public interface ApprovalRecruitmentTypeRuleRepository
        extends JpaRepository<ApprovalRecruitmentTypeRuleEntity, String> {

    @Query("""
            select (count(rule) > 0)
            from ApprovalRecruitmentTypeRuleEntity rule, EmployeeEntity employee
            where employee.employeeId = :employeeId
              and rule.active = true
              and upper(trim(rule.recruitmentTypeCode)) = upper(trim(employee.recruitmentType))
            """)
    boolean isEnabledForEmployee(@Param("employeeId") Long employeeId);
}
