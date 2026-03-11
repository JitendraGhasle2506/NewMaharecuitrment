package com.maharecruitment.gov.in.recruitment.service.impl;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationQueryService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView.DesignationVacancyView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;

@Service
@Transactional(readOnly = true)
public class RecruitmentAgencyNotificationQueryServiceImpl implements RecruitmentAgencyNotificationQueryService {

    private static final Set<AgencyNotificationTrackingStatus> VISIBLE_TRACKING_STATUSES = Set.of(
            AgencyNotificationTrackingStatus.RELEASED,
            AgencyNotificationTrackingStatus.READ,
            AgencyNotificationTrackingStatus.RESPONDED);

    private static final Set<RecruitmentNotificationStatus> VISIBLE_NOTIFICATION_STATUSES = Set.of(
            RecruitmentNotificationStatus.PENDING_ALLOCATION,
            RecruitmentNotificationStatus.IN_PROGRESS);

    private final AgencyNotificationTrackingRepository trackingRepository;
    private final AgencyMasterRepository agencyRepository;
    private final RecruitmentNotificationRepository notificationRepository;

    public RecruitmentAgencyNotificationQueryServiceImpl(
            AgencyNotificationTrackingRepository trackingRepository,
            AgencyMasterRepository agencyRepository,
            RecruitmentNotificationRepository notificationRepository) {
        this.trackingRepository = trackingRepository;
        this.agencyRepository = agencyRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public List<AgencyVisibleNotificationView> getVisibleNotifications(Long agencyId) {
        if (agencyId == null) {
            throw new RecruitmentNotificationException("Agency id is required.");
        }

        if (!agencyRepository.existsById(agencyId)) {
            throw new RecruitmentNotificationException("Agency not found for id: " + agencyId);
        }

        return trackingRepository.findVisibleTrackingByAgency(
                agencyId,
                VISIBLE_TRACKING_STATUSES,
                VISIBLE_NOTIFICATION_STATUSES)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public AgencyNotificationDetailView getNotificationDetail(Long recruitmentNotificationId, Long agencyId) {
        if (recruitmentNotificationId == null) {
            throw new RecruitmentNotificationException("Recruitment notification id is required.");
        }
        if (agencyId == null) {
            throw new RecruitmentNotificationException("Agency id is required.");
        }

        // Verify agency has access — a tracking record must exist for this agency
        AgencyNotificationTrackingEntity tracking = trackingRepository
                .findByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                        recruitmentNotificationId,
                        agencyId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Notification not released to this agency or not found."));

        RecruitmentNotificationEntity notification = notificationRepository
                .findWithVacanciesById(recruitmentNotificationId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Recruitment notification not found for id: " + recruitmentNotificationId));

        String projectName = notification.getProjectMst() != null
                ? notification.getProjectMst().getProjectName()
                : null;
        Long projectId = notification.getProjectMst() != null
                ? notification.getProjectMst().getProjectId()
                : null;

        List<DesignationVacancyView> vacancies = notification.getDesignationVacancies().stream()
                .map(this::toVacancyView)
                .toList();

        return AgencyNotificationDetailView.builder()
                .recruitmentNotificationId(notification.getRecruitmentNotificationId())
                .requestId(notification.getRequestId())
                .departmentRegistrationId(notification.getDepartmentRegistrationId())
                .departmentProjectApplicationId(notification.getDepartmentProjectApplicationId())
                .projectId(projectId)
                .projectName(projectName)
                .notificationStatus(notification.getStatus())
                .releasedRank(tracking.getReleasedRank())
                .notifiedAt(tracking.getNotifiedAt())
                .trackingStatus(tracking.getStatus())
                .designationVacancies(vacancies)
                .build();
    }

    private AgencyVisibleNotificationView toView(AgencyNotificationTrackingEntity tracking) {
        String projectName = tracking.getRecruitmentNotification().getProjectMst() != null
                ? tracking.getRecruitmentNotification().getProjectMst().getProjectName()
                : null;
        Long projectId = tracking.getRecruitmentNotification().getProjectMst() != null
                ? tracking.getRecruitmentNotification().getProjectMst().getProjectId()
                : null;

        return AgencyVisibleNotificationView.builder()
                .recruitmentNotificationId(tracking.getRecruitmentNotification().getRecruitmentNotificationId())
                .requestId(tracking.getRecruitmentNotification().getRequestId())
                .departmentRegistrationId(tracking.getRecruitmentNotification().getDepartmentRegistrationId())
                .departmentProjectApplicationId(
                        tracking.getRecruitmentNotification().getDepartmentProjectApplicationId())
                .projectId(projectId)
                .projectName(projectName)
                .releasedRank(tracking.getReleasedRank())
                .notifiedAt(tracking.getNotifiedAt())
                .trackingStatus(tracking.getStatus())
                .notificationStatus(tracking.getRecruitmentNotification().getStatus())
                .build();
    }

    private DesignationVacancyView toVacancyView(RecruitmentDesignationVacancyEntity vacancy) {
        String designationName = vacancy.getDesignationMst() != null
                ? vacancy.getDesignationMst().getDesignationName()
                : "-";
        return DesignationVacancyView.builder()
                .vacancyId(vacancy.getRecruitmentDesignationVacancyId())
                .designationName(designationName)
                .levelCode(vacancy.getLevelCode())
                .numberOfVacancy(vacancy.getNumberOfVacancy())
                .filledPost(vacancy.getFillPost())
                .jobDescription(vacancy.getJobDescription())
                .build();
    }
}
