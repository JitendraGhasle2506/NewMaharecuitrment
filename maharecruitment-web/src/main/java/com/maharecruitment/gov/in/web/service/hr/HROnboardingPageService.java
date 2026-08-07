package com.maharecruitment.gov.in.web.service.hr;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.web.dto.agency.AgencyPreOnboardingForm;
import com.maharecruitment.gov.in.web.dto.hr.EmployeeOnboardingResult;
import com.maharecruitment.gov.in.web.service.agency.model.AgencyOnboardingCandidateView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeAgencyFilterView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeListView;
import com.maharecruitment.gov.in.web.service.hr.model.EmployeeOnboardingDetailView;

public interface HROnboardingPageService {

    List<AgencyOnboardingCandidateView> getPendingHROnboardingCandidates();

    AgencyPreOnboardingForm loadOnboardingForm(Long preOnboardingId);

    EmployeeOnboardingResult saveOnboarding(Long preOnboardingId, AgencyPreOnboardingForm form, String actorEmail);

    Page<EmployeeListView> getOnboardedEmployees(String recruitmentType, Pageable pageable);

    Page<EmployeeListView> getEmployeesByStatus(String recruitmentType, String status, Pageable pageable);

    Page<EmployeeListView> getOnboardedEmployees(String recruitmentType, String searchText, Pageable pageable);

    Page<EmployeeListView> getEmployeesByStatus(String recruitmentType, String status, String searchText, Pageable pageable);

    Page<EmployeeListView> getEmployeesByStatus(
            String recruitmentType,
            String status,
            String searchText,
            Long agencyId,
            Pageable pageable);

    List<EmployeeAgencyFilterView> getAgencyFilterOptions(String status);

    EmployeeOnboardingDetailView loadEmployeeDetail(Long employeeId);

    void markEmployeeResigned(Long employeeId);
}
