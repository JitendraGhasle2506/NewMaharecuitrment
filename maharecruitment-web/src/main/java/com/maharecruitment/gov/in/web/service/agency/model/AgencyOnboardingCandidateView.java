package com.maharecruitment.gov.in.web.service.agency.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AgencyOnboardingCandidateView(
        Long preOnboardingId,
        Long recruitmentInterviewDetailId,
        Long recruitmentNotificationId,
        String requestId,
        String projectName,
        String departmentName,
        String subDepartmentName,
        String candidateName,
        String candidateEmail,
        String candidateMobile,
        String designationName,
        String levelCode,
        LocalDate joiningDate,
        LocalDate onboardingDate,
        LocalDateTime preOnboardingSubmittedAt) {
}
