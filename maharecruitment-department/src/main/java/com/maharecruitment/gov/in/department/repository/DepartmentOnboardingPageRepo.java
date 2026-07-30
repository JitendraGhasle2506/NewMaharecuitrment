package com.maharecruitment.gov.in.department.repository;

import java.util.List;

import com.maharecruitment.gov.in.department.repository.projection.DepartmentOnboardedEmployeeView;

public interface DepartmentOnboardingPageRepo {
	
	List<DepartmentOnboardedEmployeeView> findActiveOnboardedEmployeesByDepartmentId(Long departmentId);


}
