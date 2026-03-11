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
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentDesignationVacancyEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentNotificationStatus;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentNotificationRepository;
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

    public RecruitmentNotificationServiceImpl(
            RecruitmentNotificationRepository notificationRepository,
            ProjectMstRepository projectRepository,
            ManpowerDesignationMasterRepository designationRepository) {
        this.notificationRepository = notificationRepository;
        this.projectRepository = projectRepository;
        this.designationRepository = designationRepository;
    }

    @Override
    @Transactional
    public void upsertFromAuditorApproval(AuditorApprovedNotificationCommand command) {
        validateCommand(command);

        String requestId = command.getRequestId().trim().toUpperCase(Locale.ROOT);
        if (notificationRepository.existsByRequestIdIgnoreCase(requestId)) {
            log.info("Recruitment notification already exists. requestId={}", requestId);
            return;
        }

        ProjectMst project = projectRepository.findFirstByApplicationId(command.getDepartmentProjectApplicationId())
                .orElseThrow(() -> new RecruitmentNotificationException(
                        "Project not found for application id: " + command.getDepartmentProjectApplicationId()));

        List<VacancyAggregate> aggregatedVacancies = aggregateVacancies(command.getDesignationVacancies());
        Map<Long, ManpowerDesignationMaster> designationById = resolveDesignations(aggregatedVacancies);

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
