package com.maharecruitment.gov.in.web.service.agency.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyInternalAssessmentService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateInterviewScheduleInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyInternalAssessmentProjectView;
import com.maharecruitment.gov.in.web.dto.agency.AgencyInterviewScheduleForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyInternalAssessmentPageService;

@Service
@Transactional(readOnly = true)
public class AgencyInternalAssessmentPageServiceImpl implements AgencyInternalAssessmentPageService {

    private final RecruitmentAgencyInternalAssessmentService internalAssessmentService;
    private final UserAffiliationService userAffiliationService;
    private final AgencyMasterRepository agencyMasterRepository;

    public AgencyInternalAssessmentPageServiceImpl(
            RecruitmentAgencyInternalAssessmentService internalAssessmentService,
            UserAffiliationService userAffiliationService,
            AgencyMasterRepository agencyMasterRepository) {
        this.internalAssessmentService = internalAssessmentService;
        this.userAffiliationService = userAffiliationService;
        this.agencyMasterRepository = agencyMasterRepository;
    }

    @Override
    public List<AgencyInternalAssessmentProjectView> getAssessmentSubmittedProjects(String actorEmail) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return internalAssessmentService.getAssessmentSubmittedProjects(context.agencyId());
    }

    @Override
    public List<AgencyInternalAssessmentCandidateView> getAssessmentSubmittedCandidates(
            String actorEmail,
            Long recruitmentNotificationId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return internalAssessmentService.getAssessmentSubmittedCandidates(context.agencyId(), recruitmentNotificationId);
    }

    @Override
    public AgencyInternalAssessmentDetailView getAssessmentSubmittedCandidateDetail(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return internalAssessmentService.getAssessmentSubmittedCandidateDetail(
                context.agencyId(),
                recruitmentNotificationId,
                recruitmentInterviewDetailId);
    }

    @Override
    @Transactional
    public void scheduleLevelTwoInterview(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            AgencyInterviewScheduleForm scheduleForm) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        internalAssessmentService.scheduleLevelTwoInterview(
                context.agencyId(),
                context.userId(),
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                AgencyCandidateInterviewScheduleInput.builder()
                        .interviewDateTime(scheduleForm != null && scheduleForm.getInterviewDate() != null
                                ? scheduleForm.getInterviewDate().atStartOfDay()
                                : null)
                        .interviewTimeSlot(scheduleForm != null ? scheduleForm.getInterviewTimeSlot() : null)
                        .interviewLink(scheduleForm != null ? scheduleForm.getInterviewLink() : null)
                        .interviewRemarks(scheduleForm != null ? scheduleForm.getInterviewRemarks() : null)
                        .build());
    }

    private AgencyUserContext resolveAgencyUserContext(String actorEmail) {
        if (!StringUtils.hasText(actorEmail)) {
            throw new RecruitmentNotificationException("Authenticated user is required.");
        }

        User user = userAffiliationService.loadUserByEmail(actorEmail);
        Long agencyId = userAffiliationService.resolvePrimaryAgencyId(user);
        AgencyMaster agency = agencyId == null ? null : agencyMasterRepository.findById(agencyId).orElse(null);
        if (agency == null) {
            agency = agencyMasterRepository.findByOfficialEmailIgnoreCase(user.getEmail())
                    .orElseThrow(() -> new RecruitmentNotificationException(
                            "No agency profile is linked with this login user."));
        }

        return new AgencyUserContext(user.getId(), agency.getAgencyId());
    }

    private record AgencyUserContext(Long userId, Long agencyId) {
    }
}
