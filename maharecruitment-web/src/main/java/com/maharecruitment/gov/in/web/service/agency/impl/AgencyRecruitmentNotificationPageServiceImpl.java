package com.maharecruitment.gov.in.web.service.agency.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
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
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.agency.AgencyRecruitmentNotificationPageService;
import com.maharecruitment.gov.in.web.service.agency.AgencyUserContext;
import com.maharecruitment.gov.in.web.service.storage.FileStorageService;

@Service
@Transactional(readOnly = true)
public class AgencyRecruitmentNotificationPageServiceImpl implements AgencyRecruitmentNotificationPageService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");

    private final RecruitmentAgencyNotificationQueryService queryService;
    private final RecruitmentAgencyNotificationActionService actionService;
    private final RecruitmentAgencyCandidateService candidateService;
    private final AgencyAccessService agencyAccessService;
    private final FileStorageService fileStorageService;

    public AgencyRecruitmentNotificationPageServiceImpl(
            RecruitmentAgencyNotificationQueryService queryService,
            RecruitmentAgencyNotificationActionService actionService,
            RecruitmentAgencyCandidateService candidateService,
            AgencyAccessService agencyAccessService,
            FileStorageService fileStorageService) {
        this.queryService = queryService;
        this.actionService = actionService;
        this.candidateService = candidateService;
        this.agencyAccessService = agencyAccessService;
        this.fileStorageService = fileStorageService;
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
            String search,
            Pageable pageable) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getShortlistedCandidates(context.agencyId(), recruitmentNotificationId, search, pageable);
    }

    @Override
    public List<AgencySelectedCandidateProjectView> getSelectedCandidateProjects(String actorEmail) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getSelectedCandidateProjects(context.agencyId());
    }

    @Override
    public Page<AgencySelectedCandidateView> getSelectedCandidates(String actorEmail, Long recruitmentNotificationId, String search, Pageable pageable) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        return candidateService.getSelectedCandidates(context.agencyId(), recruitmentNotificationId, search, pageable);
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
                String normalizedCandidateName = trim(candidateRow.getCandidateName());
                String normalizedEmail = trim(candidateRow.getEmail());
                String normalizedMobile = trim(candidateRow.getMobile());
                String normalizedEducation = trim(candidateRow.getCandidateEducation());
                String normalizedJoiningTime = trim(candidateRow.getJoiningTime());

                MultipartFile resumeFile = candidateRow.getResumeFile();
                FileUploadResult uploadResult = fileStorageService.store(resumeFile, "recruitment/agency-candidate-resume");
                uploadedFilePaths.add(uploadResult.fullPath());

                candidateInputs.add(AgencyCandidateSubmissionInput.builder()
                        .candidateName(normalizedCandidateName)
                        .email(normalizedEmail)
                        .mobile(normalizedMobile)
                        .candidateEducation(normalizedEducation)
                        .totalExperience(candidateRow.getTotalExp())
                        .relevantExperience(candidateRow.getRelevantExp())
                        .currentCtc(candidateRow.getCurrentCtc())
                        .resigned(Boolean.TRUE.equals(candidateRow.getResigned()))
                        .lastWorkingDay(Boolean.TRUE.equals(candidateRow.getResigned())
                                ? candidateRow.getLastWorkingDay()
                                : null)
                        .joiningTime(normalizedJoiningTime)
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
        candidateService.withdrawCandidate(recruitmentNotificationId, recruitmentInterviewDetailId, context.agencyId());
    }

    @Override
    @Transactional
    public void forwardInterviewRequest(
            String actorEmail,
            Long recruitmentNotificationId,
            Long recruitmentInterviewDetailId) {
        AgencyUserContext context = resolveAgencyUserContext(actorEmail);
        candidateService.forwardInterviewRequest(
                recruitmentNotificationId,
                recruitmentInterviewDetailId,
                context.agencyId());
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
        if (rowForm.getCandidateName().startsWith(" ")) {
            throw new RecruitmentNotificationException("Candidate name must not start with a space at row " + rowNumber + ".");
        }
        if (Pattern.compile("[0-9]").matcher(rowForm.getCandidateName()).find()) {
            throw new RecruitmentNotificationException("Candidate name must not contain numbers at row " + rowNumber + ".");
        }
        if (rowForm.getCandidateName().length() < 2 || rowForm.getCandidateName().length() > 100) {
            throw new RecruitmentNotificationException("Candidate name must be between 2 and 100 characters at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getEmail())) {
            throw new RecruitmentNotificationException("Candidate email is required at row " + rowNumber + ".");
        }
        if (!EMAIL_PATTERN.matcher(trim(rowForm.getEmail())).matches()) {
            throw new RecruitmentNotificationException("Candidate email must be valid at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getMobile())) {
            throw new RecruitmentNotificationException("Candidate mobile is required at row " + rowNumber + ".");
        }
        if (!MOBILE_PATTERN.matcher(trim(rowForm.getMobile())).matches()) {
            throw new RecruitmentNotificationException("Candidate mobile must be 10 digits at row " + rowNumber + ".");
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
        if (rowForm.getCurrentCtc() == null) {
            throw new RecruitmentNotificationException("Current CTC is required at row " + rowNumber + ".");
        }
        if (rowForm.getCurrentCtc().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new RecruitmentNotificationException("Current CTC cannot be negative at row " + rowNumber + ".");
        }
        if (rowForm.getResigned() == null) {
            throw new RecruitmentNotificationException(
                    "Please specify whether the candidate has resigned at row " + rowNumber + ".");
        }
        if (Boolean.TRUE.equals(rowForm.getResigned()) && rowForm.getLastWorkingDay() == null) {
            throw new RecruitmentNotificationException(
                    "Last working day is required for a resigned candidate at row " + rowNumber + ".");
        }
        if (!StringUtils.hasText(rowForm.getJoiningTime())) {
            throw new RecruitmentNotificationException("Joining time is required at row " + rowNumber + ".");
        }
        if (rowForm.getResumeFile() == null || rowForm.getResumeFile().isEmpty()) {
            throw new RecruitmentNotificationException("Resume PDF is required at row " + rowNumber + ".");
        }
    }

    private AgencyUserContext resolveAgencyUserContext(String actorEmail) {
        return agencyAccessService.requireActiveAgencyContext(actorEmail);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
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
