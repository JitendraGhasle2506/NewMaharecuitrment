package com.maharecruitment.gov.in.web.service.hr;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;

public interface HROnboardingPageService {

    List<AgencyOnboardingCandidateView> getPendingHROnboardingCandidates();

    AgencyPreOnboardingForm loadOnboardingForm(Long preOnboardingId);

    void saveOnboarding(Long preOnboardingId, AgencyPreOnboardingForm form, String actorEmail);

    Page<EmployeeListView> getOnboardedEmployees(String recruitmentType, Pageable pageable);

    Page<EmployeeListView> getEmployeesByStatus(String recruitmentType, String status, Pageable pageable);

    void markEmployeeResigned(Long employeeId);
}
