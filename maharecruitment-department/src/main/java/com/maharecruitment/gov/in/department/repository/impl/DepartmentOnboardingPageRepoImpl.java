package com.maharecruitment.gov.in.department.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.repository.DepartmentOnboardingPageRepo;
import com.maharecruitment.gov.in.department.repository.projection.DepartmentOnboardedEmployeeView;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class DepartmentOnboardingPageRepoImpl implements DepartmentOnboardingPageRepo {
	
	@PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<DepartmentOnboardedEmployeeView> findActiveOnboardedEmployeesByDepartmentId(Long departmentId) {

    	String query = """
    		    SELECT new com.maharecruitment.gov.in.department.repository.projection.DepartmentOnboardedEmployeeView(
    		        e.employeeId,
    		        e.employeeCode,
    		        e.requestId,
    		        e.fullName,
    		        e.email,
    		        e.mobile,
    		        e.levelCode,
    		        e.joiningDate,
    		        e.onboardingDate,
    		        e.resignationDate,
    		        e.status,
    		        f.designationName
    		    )
    		    FROM EmployeeEntity e
    		    LEFT JOIN e.departmentRegistration d
    		    LEFT JOIN e.designation f
    		    WHERE e.status = :status
    		    AND d.departmentRegistrationId = :departmentId
    		    ORDER BY COALESCE(e.onboardingDate, e.joiningDate) DESC, e.employeeId DESC
    		""";

    		return entityManager.createQuery(query, DepartmentOnboardedEmployeeView.class)
    		        .setParameter("status", "ACTIVE")
    		        .setParameter("departmentId", departmentId)
    		        .getResultList();
    }

}
