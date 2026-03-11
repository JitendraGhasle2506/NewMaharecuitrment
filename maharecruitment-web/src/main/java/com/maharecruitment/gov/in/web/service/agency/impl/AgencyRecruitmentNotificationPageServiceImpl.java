package com.maharecruitment.gov.in.web.service.agency.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationActionService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationQueryService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;
import com.maharecruitment.gov.in.web.service.agency.AgencyRecruitmentNotificationPageService;

@Service
@Transactional(readOnly = true)
public class AgencyRecruitmentNotificationPageServiceImpl implements AgencyRecruitmentNotificationPageService {

    private final RecruitmentAgencyNotificationQueryService queryService;
    private final RecruitmentAgencyNotificationActionService actionService;
    private final UserRepository userRepository;
    private final AgencyMasterRepository agencyMasterRepository;

    public AgencyRecruitmentNotificationPageServiceImpl(
            RecruitmentAgencyNotificationQueryService queryService,
            RecruitmentAgencyNotificationActionService actionService,
            UserRepository userRepository,
            AgencyMasterRepository agencyMasterRepository) {
        this.queryService = queryService;
        this.actionService = actionService;
        this.userRepository = userRepository;
        this.agencyMasterRepository = agencyMasterRepository;
    }

    @Override
    public List<AgencyVisibleNotificationView> getVisibleNotifications(String actorEmail) {
        Long agencyId = resolveAgencyId(actorEmail);
        return queryService.getVisibleNotifications(agencyId);
    }

    @Override
    public AgencyNotificationDetailView getNotificationDetail(String actorEmail, Long recruitmentNotificationId) {
        Long agencyId = resolveAgencyId(actorEmail);
        return queryService.getNotificationDetail(recruitmentNotificationId, agencyId);
    }

    @Override
    @Transactional
    public void markAsRead(String actorEmail, Long recruitmentNotificationId) {
        Long agencyId = resolveAgencyId(actorEmail);
        actionService.markAsRead(recruitmentNotificationId, agencyId);
    }

    @Override
    @Transactional
    public void submitResponse(String actorEmail, Long recruitmentNotificationId) {
        Long agencyId = resolveAgencyId(actorEmail);
        actionService.submitResponse(recruitmentNotificationId, agencyId);
    }

    private Long resolveAgencyId(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }

        User user = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new RecruitmentNotificationException("Authenticated user not found."));

        AgencyMaster agency = agencyMasterRepository.findByOfficialEmailIgnoreCase(user.getEmail())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "No agency profile is linked with this login user."));

        return agency.getAgencyId();
    }
}
