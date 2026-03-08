package com.maharecruitment.gov.in.auth.service;

import com.maharecruitment.gov.in.auth.dto.DepartmentRegistrationRequest;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;

public interface DepartmentRegistrationService {

    DepartmentRegistrationEntity registerDepartment(DepartmentRegistrationRequest request);
}
