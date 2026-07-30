package com.maharecruitment.gov.in.recruitment.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationMasterRepository;
import com.maharecruitment.gov.in.master.repository.ProjectMstRepository;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningRequirementEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningStatus;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.InternalVacancyOpeningRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentInterviewDetailRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankReleaseService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationService;
import com.maharecruitment.gov.in.recruitment.service.model.AuditorApprovedNotificationCommand;
import com.maharecruitment.gov.in.recruitment.service.model.DesignationVacancyInput;

@Service
@Transactional(readOnly = true)
public class RecruitmentNotificationServiceImpl implements RecruitmentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(RecruitmentNotificationServiceImpl.class);

    private final RecruitmentNotificationRepository notificationRepository;
    private final ProjectMstRepository projectRepository;
    private final ManpowerDesignationMasterRepository designationRepository;
    private final InternalVacancyOpeningRepository internalVacancyOpeningRepository;
    private final RecruitmentInterviewDetailRepository interviewDetailRepository;
    private final RecruitmentNotificationRankReleaseService rankReleaseService;

    public RecruitmentNotificationServiceImpl(
            RecruitmentNotificationRepository notificationRepository,
            ProjectMstRepository projectRepository,
            ManpowerDesignationMasterRepository designationRepository,
            InternalVacancyOpeningRepository internalVacancyOpeningRepository,
            RecruitmentInterviewDetailRepository interviewDetailRepository,
            RecruitmentNotificationRankReleaseService rankReleaseService) {
        this.notificationRepository = notificationRepository;
        this.projectRepository = projectRepository;
        this.designationRepository = designationRepository;
        this.internalVacancyOpeningRepository = internalVacancyOpeningRepository;
        this.interviewDetailRepository = interviewDetailRepository;
        this.rankReleaseService = rankReleaseService;
    }

    @Override
    @Transactional
    public void upsertFromAuditorApproval(AuditorApprovedNotificationCommand command) {
        validateCommand(command);

        String requestId = command.getRequestId().trim().toUpperCase(Locale.ROOT);
        ProjectMst project = projectRepository.findFirstByApplicationId(command.getDepartmentProjectApplicationId())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Project not found for application id: " + command.getDepartmentProjectApplicationId()));

        List<VacancyAggregate> aggregatedVacancies = aggregateVacancies(command.getDesignationVacancies());
        Map<Long, ManpowerDesignationMaster> designationById = resolveDesignations(aggregatedVacancies);
        RecruitmentNotificationEntity existingNotification = notificationRepository.findByRequestIdIgnoreCase(requestId)
                .orElse(null);

        if (existingNotification != null) {
            if (existingNotification.getDepartmentProjectApplicationId() != null
                    && !command.getDepartmentProjectApplicationId()
                            .equals(existingNotification.getDepartmentProjectApplicationId())) {
                throw new RecruitmentNotificationException(
                        "Request id is already linked to a different department application.");
            }

            if (existingNotification.getStatus() != RecruitmentNotificationStatus.CLOSED) {
                log.info("Recruitment notification already exists. requestId={}", requestId);
                return;
            }

            existingNotification.setDepartmentRegistrationId(command.getDepartmentRegistrationId());
            existingNotification.setDepartmentProjectApplicationId(command.getDepartmentProjectApplicationId());
            existingNotification.setProjectMst(project);
            existingNotification.setStatus(RecruitmentNotificationStatus.PENDING_ALLOCATION);
            existingNotification.replaceDesignationVacancies(
                    toVacancyEntities(aggregatedVacancies, designationById));
            notificationRepository.save(existingNotification);
            log.info("Recruitment notification reopened. requestId={}, applicationId={}, vacancyCount={}",
                    requestId,
                    command.getDepartmentProjectApplicationId(),
                    aggregatedVacancies.size());
            return;
        }

        RecruitmentNotificationEntity notification = new RecruitmentNotificationEntity();
        notification.setRequestId(requestId);
        notification.setDepartmentRegistrationId(command.getDepartmentRegistrationId());
        notification.setDepartmentProjectApplicationId(command.getDepartmentProjectApplicationId());
        notification.setProjectMst(project);
        notification.setStatus(RecruitmentNotificationStatus.PENDING_ALLOCATION);
        notification.replaceDesignationVacancies(toVacancyEntities(aggregatedVacancies, designationById));

        try {
            notificationRepository.save(notification);
            log.info(
                    "Recruitment notification created. requestId={}, applicationId={}, vacancyCount={}",
                    requestId,
                    command.getDepartmentProjectApplicationId(),
                    aggregatedVacancies.size());
        } catch (DataIntegrityViolationException ex) {
            if (notificationRepository.existsByRequestIdIgnoreCase(requestId)) {
                log.info("Recruitment notification already created by parallel request. requestId={}", requestId);
                return;
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public void upsertFromInternalVacancyOpening(Long internalVacancyOpeningId) {
        InternalVacancyOpeningEntity opening = resolveOpenInternalVacancyOpening(internalVacancyOpeningId);
        RecruitmentNotificationEntity existingNotification = findExistingInternalNotification(opening);
        if (existingNotification != null) {
            syncExistingInternalNotification(existingNotification, opening);
            RecruitmentNotificationEntity savedNotification = notificationRepository.saveAndFlush(existingNotification);
            int releasedRankCount = rankReleaseService
                    .releaseEligibleRanksForNotification(savedNotification.getRecruitmentNotificationId());
            log.info(
                    "Recruitment notification synchronized for internal vacancy opening. requestId={}, openingId={}, notificationId={}, releasedRankCount={}",
                    opening.getRequestId(),
                    opening.getInternalVacancyOpeningId(),
                    savedNotification.getRecruitmentNotificationId(),
                    releasedRankCount);
            return;
        }

        RecruitmentNotificationEntity notification = new RecruitmentNotificationEntity();
        notification.setRequestId(opening.getRequestId());
        notification.setInternalVacancyOpening(opening);
        notification.setDepartmentRegistrationId(null);
        notification.setDepartmentProjectApplicationId(null);
        notification.setProjectMst(opening.getProjectMst());
        notification.setStatus(RecruitmentNotificationStatus.PENDING_ALLOCATION);
        notification.replaceDesignationVacancies(toInternalVacancyEntities(opening.getRequirements()));

        RecruitmentNotificationEntity savedNotification = saveInternalNotification(notification, opening);
        int releasedRankCount = rankReleaseService
                .releaseEligibleRanksForNotification(savedNotification.getRecruitmentNotificationId());

        log.info(
                "Recruitment notification created for internal vacancy opening. requestId={}, openingId={}, notificationId={}, vacancyCount={}, releasedRankCount={}",
                opening.getRequestId(),
                opening.getInternalVacancyOpeningId(),
                savedNotification.getRecruitmentNotificationId(),
                savedNotification.getDesignationVacancies().size(),
                releasedRankCount);
    }

    @Override
    @Transactional
    public void closeFromInternalVacancyOpening(Long internalVacancyOpeningId) {
        if (internalVacancyOpeningId == null || internalVacancyOpeningId < 1) {
            throw new RecruitmentNotificationException("Valid internal vacancy opening id is required.");
        }

        RecruitmentNotificationEntity notification = notificationRepository
                .findByInternalVacancyOpeningInternalVacancyOpeningId(internalVacancyOpeningId)
                .orElse(null);
        if (notification == null) {
            return;
        }

        if (notification.getStatus() != RecruitmentNotificationStatus.CLOSED) {
            notification.setStatus(RecruitmentNotificationStatus.CLOSED);
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void closeFromDepartmentProjectApplication(Long departmentProjectApplicationId) {
        if (departmentProjectApplicationId == null || departmentProjectApplicationId < 1) {
            return;
        }

        RecruitmentNotificationEntity notification = notificationRepository
                .findByDepartmentProjectApplicationId(departmentProjectApplicationId)
                .orElse(null);
        if (notification == null) {
            return;
        }

        if (notification.getStatus() != RecruitmentNotificationStatus.CLOSED) {
            notification.setStatus(RecruitmentNotificationStatus.CLOSED);
            notificationRepository.save(notification);
            log.info("Recruitment notification closed for department application. requestId={}, applicationId={}",
                    notification.getRequestId(),
                    departmentProjectApplicationId);
        }
    }

    private void validateCommand(AuditorApprovedNotificationCommand command) {
        if (command == null) {
            throw new RecruitmentNotificationException("Notification command is required.");
        }
        if (!StringUtils.hasText(command.getRequestId())) {
            throw new RecruitmentNotificationException("Request id is required for recruitment notification.");
        }
        if (command.getDepartmentRegistrationId() == null) {
            throw new RecruitmentNotificationException(
                    "Department registration id is required for recruitment notification.");
        }
        if (command.getDepartmentProjectApplicationId() == null) {
            throw new RecruitmentNotificationException(
                    "Department application id is required for recruitment notification.");
        }
        if (command.getDesignationVacancies() == null || command.getDesignationVacancies().isEmpty()) {
            throw new RecruitmentNotificationException("At least one designation vacancy is required.");
        }
    }

    private InternalVacancyOpeningEntity resolveOpenInternalVacancyOpening(Long internalVacancyOpeningId) {
        if (internalVacancyOpeningId == null || internalVacancyOpeningId < 1) {
            throw new RecruitmentNotificationException("Valid internal vacancy opening id is required.");
        }

        InternalVacancyOpeningEntity opening = internalVacancyOpeningRepository
                .findDetailedByInternalVacancyOpeningId(internalVacancyOpeningId)
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Internal vacancy opening not found for id: " + internalVacancyOpeningId));

        if (opening.getStatus() != InternalVacancyOpeningStatus.OPEN) {
            throw new RecruitmentNotificationException(
                    "Recruitment notification can be created only for submitted internal vacancy openings.");
        }
        if (!StringUtils.hasText(opening.getRequestId())) {
            throw new RecruitmentNotificationException("Request id is required for internal vacancy notification.");
        }
        if (opening.getProjectMst() == null || opening.getProjectMst().getProjectId() == null) {
            throw new RecruitmentNotificationException("Project is required for internal vacancy notification.");
        }
        if (opening.getRequirements() == null || opening.getRequirements().isEmpty()) {
            throw new RecruitmentNotificationException(
                    "At least one designation vacancy is required for internal vacancy notification.");
        }

        return opening;
    }

    private RecruitmentNotificationEntity findExistingInternalNotification(InternalVacancyOpeningEntity opening) {
        RecruitmentNotificationEntity notification = notificationRepository
                .findByInternalVacancyOpeningInternalVacancyOpeningId(opening.getInternalVacancyOpeningId())
                .orElse(null);
        if (notification != null) {
            return notification;
        }

        if (!StringUtils.hasText(opening.getRequestId())) {
            return null;
        }

        RecruitmentNotificationEntity byRequestId = notificationRepository
                .findByRequestIdIgnoreCase(opening.getRequestId())
                .orElse(null);
        if (byRequestId != null
                && byRequestId.getInternalVacancyOpening() != null
                && !opening.getInternalVacancyOpeningId()
                        .equals(byRequestId.getInternalVacancyOpening().getInternalVacancyOpeningId())) {
            throw new RecruitmentNotificationException(
                    "Request id is already linked to a different internal vacancy opening.");
        }
        return byRequestId;
    }

    private void syncExistingInternalNotification(
            RecruitmentNotificationEntity notification,
            InternalVacancyOpeningEntity opening) {
        if (notification.getInternalVacancyOpening() != null
                && !opening.getInternalVacancyOpeningId()
                        .equals(notification.getInternalVacancyOpening().getInternalVacancyOpeningId())) {
            throw new RecruitmentNotificationException(
                    "Request id is already linked to a different internal vacancy opening.");
        }

        notification.setInternalVacancyOpening(opening);
        notification.setProjectMst(opening.getProjectMst());
        syncInternalVacancyDesignationVacancies(notification, opening);
        if (notification.getStatus() == null || notification.getStatus() == RecruitmentNotificationStatus.CLOSED) {
            notification.setStatus(RecruitmentNotificationStatus.PENDING_ALLOCATION);
        }
    }

    private RecruitmentNotificationEntity saveInternalNotification(
            RecruitmentNotificationEntity notification,
            InternalVacancyOpeningEntity opening) {
        try {
            return notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException ex) {
            RecruitmentNotificationEntity existingNotification = findExistingInternalNotification(opening);
            if (existingNotification != null) {
                return existingNotification;
            }
            throw ex;
        }
    }

    private void syncInternalVacancyDesignationVacancies(
            RecruitmentNotificationEntity notification,
            InternalVacancyOpeningEntity opening) {
        List<RecruitmentDesignationVacancyEntity> updatedVacancies = toInternalVacancyEntities(opening.getRequirements());
        if (hasSameVacancySnapshot(notification.getDesignationVacancies(), updatedVacancies)) {
            return;
        }

        if (notification.getRecruitmentNotificationId() != null
                && interviewDetailRepository.existsByRecruitmentNotificationRecruitmentNotificationIdAndActiveTrue(
                        notification.getRecruitmentNotificationId())) {
            throw new RecruitmentNotificationException(
                    "Submitted candidate data already exists for this internal vacancy opening. Vacancy rows cannot be changed after submission.");
        }

        notification.replaceDesignationVacancies(updatedVacancies);
    }

    private boolean hasSameVacancySnapshot(
            List<RecruitmentDesignationVacancyEntity> currentVacancies,
            List<RecruitmentDesignationVacancyEntity> updatedVacancies) {
        return toVacancySnapshot(currentVacancies).equals(toVacancySnapshot(updatedVacancies));
    }

    private Map<String, Long> toVacancySnapshot(List<RecruitmentDesignationVacancyEntity> vacancies) {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        if (vacancies == null || vacancies.isEmpty()) {
            return snapshot;
        }

        for (RecruitmentDesignationVacancyEntity vacancy : vacancies) {
            if (vacancy == null
                    || vacancy.getDesignationMst() == null
                    || vacancy.getDesignationMst().getDesignationId() == null
                    || !StringUtils.hasText(vacancy.getLevelCode())) {
                continue;
            }

            snapshot.put(
                    vacancy.getDesignationMst().getDesignationId()
                            + "|" + vacancy.getLevelCode().trim().toUpperCase(Locale.ROOT),
                    vacancy.getNumberOfVacancy());
        }
        return snapshot;
    }

    private List<VacancyAggregate> aggregateVacancies(List<DesignationVacancyInput> vacancies) {
        Map<String, VacancyAggregate> aggregateByKey = new LinkedHashMap<>();

        for (DesignationVacancyInput vacancy : vacancies) {
            validateVacancy(vacancy);

            Long designationId = vacancy.getDesignationId();
            String levelCode = vacancy.getLevelCode().trim().toUpperCase(Locale.ROOT);
            Long vacancyCount = vacancy.getNumberOfVacancy();
            String jobDescription = normalizeText(vacancy.getJobDescription());

            String key = designationId + "|" + levelCode;
            VacancyAggregate aggregate = aggregateByKey.computeIfAbsent(
                    key,
                    ignored -> new VacancyAggregate(designationId, levelCode, 0L, null));

            aggregate.setNumberOfVacancy(aggregate.getNumberOfVacancy() + vacancyCount);
            if (aggregate.getJobDescription() == null && jobDescription != null) {
                aggregate.setJobDescription(jobDescription);
            }
        }

        return new ArrayList<>(aggregateByKey.values());
    }

    private void validateVacancy(DesignationVacancyInput vacancy) {
        if (vacancy == null) {
            throw new RecruitmentNotificationException("Designation vacancy row is invalid.");
        }
        if (vacancy.getDesignationId() == null) {
            throw new RecruitmentNotificationException("Designation id is required for each vacancy row.");
        }
        if (!StringUtils.hasText(vacancy.getLevelCode())) {
            throw new RecruitmentNotificationException("Level code is required for each vacancy row.");
        }
        if (vacancy.getNumberOfVacancy() == null || vacancy.getNumberOfVacancy() <= 0) {
            throw new RecruitmentNotificationException("Number of vacancy must be greater than zero.");
        }
    }

    private Map<Long, ManpowerDesignationMaster> resolveDesignations(List<VacancyAggregate> vacancies) {
        Set<Long> designationIds = vacancies.stream()
                .map(VacancyAggregate::getDesignationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, ManpowerDesignationMaster> designationById = designationRepository.findAllById(designationIds)
                .stream()
                .collect(Collectors.toMap(
                        ManpowerDesignationMaster::getDesignationId,
                        designation -> designation,
                        (first, second) -> first,
                        LinkedHashMap::new));

        if (designationById.size() != designationIds.size()) {
            Set<Long> missingIds = new LinkedHashSet<>(designationIds);
            missingIds.removeAll(designationById.keySet());
            throw new RecruitmentNotificationException("Designation master not found for ids: " + missingIds);
        }

        return designationById;
    }

    private List<RecruitmentDesignationVacancyEntity> toVacancyEntities(
            List<VacancyAggregate> vacancies,
            Map<Long, ManpowerDesignationMaster> designationById) {
        List<RecruitmentDesignationVacancyEntity> entities = new ArrayList<>();

        for (VacancyAggregate vacancy : vacancies) {
            RecruitmentDesignationVacancyEntity entity = new RecruitmentDesignationVacancyEntity();
            entity.setDesignationMst(designationById.get(vacancy.getDesignationId()));
            entity.setLevelCode(vacancy.getLevelCode());
            entity.setNumberOfVacancy(vacancy.getNumberOfVacancy());
            entity.setJobDescription(vacancy.getJobDescription());
            entity.setFillPost(0L);
            entities.add(entity);
        }
        return entities;
    }

    private List<RecruitmentDesignationVacancyEntity> toInternalVacancyEntities(
            List<InternalVacancyOpeningRequirementEntity> requirements) {
        Map<String, RecruitmentDesignationVacancyEntity> vacancyByKey = new LinkedHashMap<>();

        for (InternalVacancyOpeningRequirementEntity requirement : requirements) {
            validateInternalRequirement(requirement);

            Long designationId = requirement.getDesignationMst().getDesignationId();
            String levelCode = requirement.getLevelCode().trim().toUpperCase(Locale.ROOT);
            String key = designationId + "|" + levelCode;

            RecruitmentDesignationVacancyEntity vacancy = vacancyByKey.computeIfAbsent(key, ignored -> {
                RecruitmentDesignationVacancyEntity entity = new RecruitmentDesignationVacancyEntity();
                entity.setDesignationMst(requirement.getDesignationMst());
                entity.setLevelCode(levelCode);
                entity.setNumberOfVacancy(0L);
                entity.setFillPost(0L);
                return entity;
            });

            vacancy.setNumberOfVacancy(vacancy.getNumberOfVacancy() + requirement.getNumberOfVacancy());
        }

        return new ArrayList<>(vacancyByKey.values());
    }

    private void validateInternalRequirement(InternalVacancyOpeningRequirementEntity requirement) {
        if (requirement == null || requirement.getDesignationMst() == null
                || requirement.getDesignationMst().getDesignationId() == null) {
            throw new RecruitmentNotificationException("Internal vacancy designation is required.");
        }
        if (!StringUtils.hasText(requirement.getLevelCode())) {
            throw new RecruitmentNotificationException("Internal vacancy level code is required.");
        }
        if (requirement.getNumberOfVacancy() == null || requirement.getNumberOfVacancy() <= 0) {
            throw new RecruitmentNotificationException(
                    "Internal vacancy number of vacancy must be greater than zero.");
        }
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static final class VacancyAggregate {
        private final Long designationId;
        private final String levelCode;
        private Long numberOfVacancy;
        private String jobDescription;

        private VacancyAggregate(Long designationId, String levelCode, Long numberOfVacancy, String jobDescription) {
            this.designationId = designationId;
            this.levelCode = levelCode;
            this.numberOfVacancy = numberOfVacancy;
            this.jobDescription = jobDescription;
        }

        private Long getDesignationId() {
            return designationId;
        }

        private String getLevelCode() {
            return levelCode;
        }

        private Long getNumberOfVacancy() {
            return numberOfVacancy;
        }

        private void setNumberOfVacancy(Long numberOfVacancy) {
            this.numberOfVacancy = numberOfVacancy;
        }

        private String getJobDescription() {
            return jobDescription;
        }

        private void setJobDescription(String jobDescription) {
            this.jobDescription = jobDescription;
        }
    }
}
