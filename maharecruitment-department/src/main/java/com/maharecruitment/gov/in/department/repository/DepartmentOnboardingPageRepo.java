package com.maharecruitment.gov.in.department.repository;

import java.util.List;

public interface DepartmentOnboardingPageRepo {
	
	List<Object[]> getOnboardedEmployees(Long departmentId);


}
