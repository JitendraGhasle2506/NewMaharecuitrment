package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.agency.AgencyUserContext;
import com.maharecruitment.gov.in.web.service.dashboard.AgencyDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.AgencyDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.AgencyTaskView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgencyDashboardServiceImpl implements AgencyDashboardService {

    private final AgencyAccessService agencyAccessService;
    private final AgencyNotificationTrackingRepository agencyNotificationTrackingRepository;
    private final AgencyCandidatePreOnboardingRepository agencyCandidatePreOnboardingRepository;
    private final EmployeeRepository employeeRepository;
    private final RecruitmentInterviewDetailRepository recruitmentInterviewDetailRepository;

    @Override
    public AgencyDashboardView getDashboard() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            return emptyDashboard();
        }

        AgencyUserContext context = agencyAccessService.requireActiveAgencyContext(userDetails.getUsername());
        Long agencyId = context.agencyId();
        String agencyName = context.agencyName();

        long totalOpenings = agencyNotificationTrackingRepository.countByAgencyAgencyIdAndStatus(agencyId,
                AgencyNotificationTrackingStatus.RELEASED);
        long candidatesSubmitted = agencyCandidatePreOnboardingRepository.countByInterviewDetailAgencyAgencyId(agencyId);
        long interviewsScheduled = recruitmentInterviewDetailRepository
                .countByAgencyAgencyIdAndInterviewDateTimeIsNotNull(agencyId);
        long onboardedEmployees = employeeRepository.countByAgencyAgencyId(agencyId);

        List<AgencyNotificationTrackingEntity> recentTrackings = agencyNotificationTrackingRepository
                .findVisibleTrackingByAgency(agencyId,
                        List.of(AgencyNotificationTrackingStatus.RELEASED, AgencyNotificationTrackingStatus.READ,
                                AgencyNotificationTrackingStatus.RESPONDED),
                        List.of(RecruitmentNotificationStatus.PENDING_ALLOCATION,
                                RecruitmentNotificationStatus.IN_PROGRESS, RecruitmentNotificationStatus.CLOSED));

        List<AgencyTaskView> recentNotifications = recentTrackings.stream()
                .limit(5)
                .map(t -> new AgencyTaskView(
                        t.getRecruitmentNotification().getRequestId(),
                        t.getRecruitmentNotification().getProjectMst().getProjectName(),
                        t.getStatus().name()))
                .toList();

        return new AgencyDashboardView(
                agencyName,
                totalOpenings,
                candidatesSubmitted,
                interviewsScheduled,
                onboardedEmployees,
                "Active",
                recentNotifications);
    }

    private AgencyDashboardView emptyDashboard() {
        return new AgencyDashboardView("Unknown Agency", 0, 0, 0, 0, "Inactive", List.of());
    }
}
