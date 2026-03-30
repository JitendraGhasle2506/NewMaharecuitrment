package com.maharecruitment.gov.in.department.service;

import java.util.List;

import com.maharecruitment.gov.in.department.repository.projection.DepartmentOnboardedEmployeeView;


public interface DepartmentOnboardingPageService {

	
	List<DepartmentOnboardedEmployeeView> getOnboardedEmployees(String actorEmail);
	
}
