package com.maharecruitment.gov.in.web.service.dashboard.impl;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.projection.AgencyVisibleNotificationProjection;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.agency.AgencyUserContext;
import com.maharecruitment.gov.in.web.service.dashboard.AgencyDashboardService;
import com.maharecruitment.gov.in.web.service.dashboard.model.AgencyDashboardView;
import com.maharecruitment.gov.in.web.service.dashboard.model.AgencyTaskView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgencyDashboardServiceImpl implements AgencyDashboardService {

    private static final Pageable RECENT_NOTIFICATIONS_PAGE = PageRequest.of(0, 5);

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

        List<AgencyTaskView> recentNotifications = agencyNotificationTrackingRepository
                .findRecentVisibleNotificationsByAgency(agencyId,
                        List.of(AgencyNotificationTrackingStatus.RELEASED, AgencyNotificationTrackingStatus.READ,
                                AgencyNotificationTrackingStatus.RESPONDED),
                        List.of(RecruitmentNotificationStatus.PENDING_ALLOCATION,
                                RecruitmentNotificationStatus.IN_PROGRESS, RecruitmentNotificationStatus.CLOSED),
                        RECENT_NOTIFICATIONS_PAGE)
                .stream()
                .map(this::toTaskView)
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

    private AgencyTaskView toTaskView(AgencyVisibleNotificationProjection notification) {
        return new AgencyTaskView(
                notification.getRequestId(),
                notification.getProjectName(),
                notification.getTrackingStatus() != null ? notification.getTrackingStatus().name() : "");
    }

    private AgencyDashboardView emptyDashboard() {
        return new AgencyDashboardView("Unknown Agency", 0, 0, 0, 0, "Inactive", List.of());
    }
}
