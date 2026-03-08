package com.maharecruitment.gov.in.web.service.registration;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationForm;

public interface DepartmentRegistrationPageService {

    DepartmentRegistrationEntity register(DepartmentRegistrationForm form);
}
