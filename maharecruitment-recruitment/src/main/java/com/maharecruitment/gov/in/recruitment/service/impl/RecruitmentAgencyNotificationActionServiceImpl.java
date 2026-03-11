package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingEntity;
import com.maharecruitment.gov.in.recruitment.entity.AgencyNotificationTrackingStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyNotificationTrackingRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationActionService;

@Service
@Transactional(readOnly = true)
public class RecruitmentAgencyNotificationActionServiceImpl implements RecruitmentAgencyNotificationActionService {

    private final AgencyNotificationTrackingRepository trackingRepository;
    private final RecruitmentNotificationRepository notificationRepository;

    public RecruitmentAgencyNotificationActionServiceImpl(
            AgencyNotificationTrackingRepository trackingRepository,
            RecruitmentNotificationRepository notificationRepository) {
        this.trackingRepository = trackingRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void markAsRead(Long recruitmentNotificationId, Long agencyId) {
        AgencyNotificationTrackingEntity tracking = getTracking(recruitmentNotificationId, agencyId);

        if (tracking.getStatus() == AgencyNotificationTrackingStatus.RELEASED) {
            tracking.setStatus(AgencyNotificationTrackingStatus.READ);
            tracking.setReadAt(LocalDateTime.now());
            trackingRepository.save(tracking);
        }
    }

    @Override
    @Transactional
    public void submitResponse(Long recruitmentNotificationId, Long agencyId) {
        RecruitmentNotificationEntity notification = notificationRepository
                .findByIdForUpdate(recruitmentNotificationId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Recruitment notification not found for id: " + recruitmentNotificationId));

        if (notification.getStatus() == RecruitmentNotificationStatus.CLOSED) {
            throw new RecruitmentNotificationException("Notification is already closed.");
        }

        AgencyNotificationTrackingEntity tracking = trackingRepository
                .findByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                        recruitmentNotificationId,
                        agencyId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Notification is not released for agency id: " + agencyId));

        if (tracking.getStatus() == AgencyNotificationTrackingStatus.RESPONDED) {
            return;
        }

        tracking.setStatus(AgencyNotificationTrackingStatus.RESPONDED);
        tracking.setRespondedAt(LocalDateTime.now());
        trackingRepository.save(tracking);

        if (notification.getStatus() != RecruitmentNotificationStatus.IN_PROGRESS) {
            notification.setStatus(RecruitmentNotificationStatus.IN_PROGRESS);
            notificationRepository.save(notification);
        }
    }

    private AgencyNotificationTrackingEntity getTracking(Long recruitmentNotificationId, Long agencyId) {
        if (recruitmentNotificationId == null) {
            throw new RecruitmentNotificationException("Recruitment notification id is required.");
        }
        if (agencyId == null) {
            throw new RecruitmentNotificationException("Agency id is required.");
        }

        return trackingRepository
                .findByRecruitmentNotificationRecruitmentNotificationIdAndAgencyAgencyId(
                        recruitmentNotificationId,
                        agencyId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Notification is not released for agency id: " + agencyId));
    }
}
