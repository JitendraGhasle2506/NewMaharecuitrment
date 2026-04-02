package com.maharecruitment.gov.in.web.service.agency.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.service.UserAffiliationService;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyCandidatePreOnboardingRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyCandidateService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationActionService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentAgencyNotificationQueryService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateInterviewScheduleInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyCandidateSubmissionInput;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyNotificationDetailView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyShortlistedCandidateProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateProjectView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySelectedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyShortlistedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencySubmittedCandidateView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationListMetricsView;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyVisibleNotificationView;
import com.maharecruitment.gov.in.web.dto.FileUploadResult;
import com.maharecruitment.gov.in.web.dto.agency.AgencyCandidateBatchForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyCandidateRowForm;
import com.maharecruitment.gov.in.web.dto.agency.AgencyInterviewScheduleForm;
import com.maharecruitment.gov.in.web.service.agency.AgencyRecruitmentNotificationPageService;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
@Transactional(readOnly = true)
public class AgencyRecruitmentNotificationPageServiceImpl implements AgencyRecruitmentNotificationPageService {

    private final RecruitmentAgencyNotificationQueryService queryService;
    private final RecruitmentAgencyNotificationActionService actionService;
    private final RecruitmentAgencyCandidateService candidateService;
    private final UserAffiliationService userAffiliationService;
    private final AgencyMasterRepository agencyMasterRepository;
    private final FileStorageService fileStorageService;
    private final AgencyCandidatePreOnboardingRepository preOnboardingRepository;

    public AgencyRecruitmentNotificationPageServiceImpl(
            RecruitmentAgencyNotificationQueryService queryService,
            RecruitmentAgencyNotificationActionService actionService,
            RecruitmentAgencyCandidateService candidateService,
            UserAffiliationService userAffiliationService,
            AgencyMasterRepository agencyMasterRepository,
            FileStorageService fileStorageService,
            AgencyCandidatePreOnboardingRepository preOnboardingRepository) {
        this.queryService = queryService;
        this.actionService = actionService;
        this.candidateService = candidateService;
        this.userAffiliationService = userAffiliationService;
        this.agencyMasterRepository = agencyMasterRepository;
        this.fileStorageService = fileStorageService;
        this.preOnboardingRepository = preOnboardingRepository;
    }

    @Override
    public Page<AgencyVisibleNotificationView> getVisibleNotifications(
            String actorEmail,
            String searchText,
            Pageable pageable) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return queryService.getVisibleNotifications(context.agencyId(), searchText, pageable);
    }

    @Override
    public AgencyVisibleNotificationListMetricsView getVisibleNotificationMetrics(String actorEmail, String searchText) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return queryService.getVisibleNotificationMetrics(context.agencyId(), searchText);
    }

    @Override
    public AgencyNotificationDetailView getNotificationDetail(String actorEmail, Long recruitmentNotificationId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return queryService.getNotificationDetail(recruitmentNotificationId, context.agencyId());
    }

    @Override
    @Transactional
    public void markAsRead(String actorEmail, Long recruitmentNotificationId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        actionService.markAsRead(recruitmentNotificationId, context.agencyId());
    }

    @Override
    @Transactional
    public void submitResponse(String actorEmail, Long recruitmentNotificationId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        actionService.submitResponse(recruitmentNotificationId, context.agencyId());
    }

    @Override
    public List<AgencySubmittedCandidateView> getSubmittedCandidates(String actorEmail, Long recruitmentNotificationId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getSubmittedCandidates(recruitmentNotificationId, context.agencyId());
    }

    @Override
    public List<AgencyShortlistedCandidateProjectView> getShortlistedCandidateProjects(String actorEmail) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getShortlistedCandidateProjects(context.agencyId());
    }

    @Override
    public List<AgencyShortlistedCandidateView> getShortlistedCandidates(String actorEmail) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getShortlistedCandidates(context.agencyId());
    }

    @Override
    public Page<AgencyShortlistedCandidateView> getShortlistedCandidates(
            String actorEmail,
            Long recruitmentNotificationId,
            Pageable pageable) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getShortlistedCandidates(context.agencyId(), recruitmentNotificationId, pageable);
    }

    @Override
    public List<AgencySelectedCandidateProjectView> getSelectedCandidateProjects(String actorEmail) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getSelectedCandidateProjects(context.agencyId());
    }

    @Override
    public List<AgencySelectedCandidateView> getSelectedCandidates(String actorEmail, Long recruitmentNotificationId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getSelectedCandidates(context.agencyId(), recruitmentNotificationId);
    }

    @Override
    @Transactional
    public void submitCandidates(
            String actorEmail,
            Long recruitmentNotificationId,
            AgencyCandidateBatchForm candidateBatchForm) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        validateBatchForm(candidateBatchForm);

        List<String> uploadedFilePaths = new ArrayList<>();
        List<AgencyCandidateSubmissionInput> candidateInputs = new ArrayList<>();

        try {
            for (int index = 0; index < candidateBatchForm.getCandidates().size(); index++) {
                AgencyCandidateRowForm candidateRow = candidateBatchForm.getCandidates().get(index);
                int rowNumber = index + 1;
                validateRowForm(candidateRow, rowNumber);

                MultipartFile resumeFile = candidateRow.getResumeFile();
                FileUploadResult uploadResult = fileStorageService.store(resumeFile, "recruitment/agency-candidate-resume");
                uploadedFilePaths.add(uploadResult.fullPath());

                candidateInputs.add(AgencyCandidateSubmissionInput.builder()
                        .candidateName(candidateRow.getCandidateName())
                        .email(candidateRow.getEmail())
                        .mobile(candidateRow.getMobile())
                        .candidateEducation(candidateRow.getCandidateEducation())
                        .totalExperience(candidateRow.getTotalExp())
                        .relevantExperience(candidateRow.getRelevantExp())
                        .joiningTime(candidateRow.getJoiningTime())
                        .resumeOriginalName(uploadResult.originalFileName())
                        .resumeFilePath(uploadResult.fullPath())
                        .resumeFileType(uploadResult.contentType())
                        .resumeFileSize(uploadResult.size())
                        .build());
            }

            candidateService.submitCandidates(
                    recruitmentNotificationId,
                    context.agencyId(),
                    context.userId(),
                    candidateBatchForm.getDesignationVacancyId(),
                    candidateInputs);
        } catch (RuntimeException ex) {
            uploadedFilePaths.forEach(fileStorageService::deleteQuietly);
            throw ex;
        }
    }

    @Override
    @Transactional
    public void scheduleInterview(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId,
            AgencyInterviewScheduleForm interviewScheduleForm) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        candidateService.scheduleInterview(
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                context.agencyId(),
                context.userId(),
                AgencyCandidateInterviewScheduleInput.builder()
                        .interviewDateTime(resolveInterviewDateTime(interviewScheduleForm))
                        .interviewTimeSlot(
                                interviewScheduleForm != null ? interviewScheduleForm.getInterviewTimeSlot() : null)
                        .interviewLink(interviewScheduleForm != null ? interviewScheduleForm.getInterviewLink() : null)
                        .interviewRemarks(
                                interviewScheduleForm != null ? interviewScheduleForm.getInterviewRemarks() : null)
                        .build());
    }

    @Override
    @Transactional
    public void withdrawCandidate(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        var preOnboarding = preOnboardingRepository.findByInterviewDetailIdAndAgencyIdForForm(
                recruitmentInterviewDetailId,
                context.agencyId()).orElse(null);
        String resumeFilePath = candidateService.getSubmittedCandidates(recruitmentNotificationId, context.agencyId())
                .stream()
                .filter(candidate -> recruitmentInterviewDetailId.equals(candidate.getRecruitmentInterviewDetailId()))
                .map(AgencySubmittedCandidateView::getResumeFilePath)
                .findFirst()
                .orElse(null);
        candidateService.withdrawCandidate(recruitmentNotificationId, recruitmentInterviewDetailId, context.agencyId());
        if (StringUtils.hasText(resumeFilePath)) {
            fileStorageService.deleteQuietly(resumeFilePath);
        }
        if (preOnboarding != null) {
            fileStorageService.deleteQuietly(preOnboarding.getAadhaarFilePath());
            fileStorageService.deleteQuietly(preOnboarding.getPanFilePath());
            fileStorageService.deleteQuietly(preOnboarding.getExperienceDocFilePath());
            fileStorageService.deleteQuietly(preOnboarding.getPhotoFilePath());
        }
    }

    private void validateBatchForm(AgencyCandidateBatchForm candidateBatchForm) {
        if (candidateBatchForm == null) {
            throw new RecruitmentNotificationException("Candidate form is required.");
        }
        if (candidateBatchForm.getDesignationVacancyId() == null || candidateBatchForm.getDesignationVacancyId() < 1) {
            throw new RecruitmentNotificationException("Designation selection is required.");
        }
        if (candidateBatchForm.getCandidates() == null || candidateBatchForm.getCandidates().isEmpty()) {
            throw new RecruitmentNotificationException("Please add at least one candidate.");
        }
    }

    private void validateRowForm(AgencyCandidateRowForm rowForm, int rowNumber) {
        if (rowForm == null) {
            throw new RecruitmentNotificationException("Invalid candidate row at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getCandidateName())) {
            throw new RecruitmentNotificationException("Candidate name is required at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getEmail())) {
            throw new RecruitmentNotificationException("Candidate email is required at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getMobile())) {
            throw new RecruitmentNotificationException("Candidate mobile is required at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getCandidateEducation())) {
            throw new RecruitmentNotificationException("Candidate qualification is required at row " + rowNumber + ".");
        }
        if (rowForm.getTotalExp() == null) {
            throw new RecruitmentNotificationException("Total experience is required at row " + rowNumber + ".");
        }
        if (rowForm.getRelevantExp() == null) {
            throw new RecruitmentNotificationException("Relevant experience is required at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getJoiningTime())) {
            throw new RecruitmentNotificationException("Joining time is required at row " + rowNumber + ".");
        }
        if (rowForm.getResumeFile() == null || rowForm.getResumeFile().isEmpty()) {
            throw new RecruitmentNotificationException("Resume PDF is required at row " + rowNumber + ".");
        }
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

    private java.time.LocalDateTime resolveInterviewDateTime(AgencyInterviewScheduleForm interviewScheduleForm) {
        if (interviewScheduleForm == null) {
            return null;
        }
        if (interviewScheduleForm.getInterviewDateTime() != null) {
            return interviewScheduleForm.getInterviewDateTime();
        }
        return interviewScheduleForm.getInterviewDate() != null
                ? interviewScheduleForm.getInterviewDate().atStartOfDay()
                : null;
    }
}
