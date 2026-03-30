package com.maharecruitment.gov.in.department.repository.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.maharecruitment.gov.in.department.repository.DepartmentOnboardingPageRepo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class DepartmentOnboardingPageRepoImpl implements DepartmentOnboardingPageRepo{
	
	@PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Object[]> getOnboardedEmployees(Long departmentId) {

    	String query = """
    		    SELECT 
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
    		    FROM EmployeeEntity e
    		    LEFT JOIN e.departmentRegistration d
    		    LEFT JOIN e.designation f
    		    WHERE e.status = :status
    		    AND d.departmentRegistrationId = :departmentId
    		""";

    		return entityManager.createQuery(query, Object[].class)
    		        .setParameter("status", "ACTIVE")
    		        .setParameter("departmentId", departmentId)
    		        .getResultList();
    }

}
