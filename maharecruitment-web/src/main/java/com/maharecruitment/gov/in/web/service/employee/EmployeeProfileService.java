package com.maharecruitment.gov.in.web.service.employee;

import com.maharecruitment.gov.in.web.service.hr.model.EmployeeOnboardingDetailView;

public interface EmployeeProfileService {

    EmployeeOnboardingDetailView loadCurrentEmployeeProfile(String loginEmail);
    
    void updateEmployeePhoto(String loginEmail, org.springframework.web.multipart.MultipartFile file);
}
