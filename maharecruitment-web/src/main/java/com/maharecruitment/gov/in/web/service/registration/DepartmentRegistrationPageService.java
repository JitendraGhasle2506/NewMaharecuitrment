package com.maharecruitment.gov.in.web.service.registration;

import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationForm;
import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationResult;

import jakarta.servlet.http.HttpSession;

public interface DepartmentRegistrationPageService {

    DepartmentRegistrationResult register(DepartmentRegistrationForm form, HttpSession session);
}
