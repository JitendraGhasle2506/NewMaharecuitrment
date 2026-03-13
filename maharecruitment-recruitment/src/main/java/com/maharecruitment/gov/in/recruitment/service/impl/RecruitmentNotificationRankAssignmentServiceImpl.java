package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.AgencyGlobalRankEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.AgencyGlobalRankRepository;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankAssignmentService;
import com.maharecruitment.gov.in.recruitment.service.RecruitmentNotificationRankReleaseService;
import com.maharecruitment.gov.in.recruitment.service.model.AgencyRankAssignmentCommand;

@Service
@Transactional(readOnly = true)
public class RecruitmentNotificationRankAssignmentServiceImpl
        implements RecruitmentNotificationRankAssignmentService {

    private final AgencyGlobalRankRepository agencyGlobalRankRepository;
    private final AgencyMasterRepository agencyRepository;
    private final RecruitmentNotificationRankReleaseService rankReleaseService;

    public RecruitmentNotificationRankAssignmentServiceImpl(
            AgencyGlobalRankRepository agencyGlobalRankRepository,
            AgencyMasterRepository agencyRepository,
            RecruitmentNotificationRankReleaseService rankReleaseService) {
        this.agencyGlobalRankRepository = agencyGlobalRankRepository;
        this.agencyRepository = agencyRepository;
        this.rankReleaseService = rankReleaseService;
    }

    @Override
    @Transactional
    public void assignAgencyRanks(Long recruitmentNotificationId, List<AgencyRankAssignmentCommand> assignments) {
        validateInput(assignments);

        Map<Long, AgencyMaster> agencyById = loadAndValidateAgencies(assignments);
        List<AgencyGlobalRankEntity> existingMappings = agencyGlobalRankRepository
                .findAllWithAgencyOrderByRankNumberAscAgencyAgencyIdAsc();
        Map<Long, AgencyGlobalRankEntity> existingByAgencyId = mapByAgency(existingMappings);

        Set<Long> requestedAgencyIds = new HashSet<>();
        List<AgencyGlobalRankEntity> entitiesToSave = new ArrayList<>();
        LocalDateTime assignedAt = LocalDateTime.now();

        for (AgencyRankAssignmentCommand assignment : assignments) {
            Long agencyId = assignment.getAgencyId();
            requestedAgencyIds.add(agencyId);

            AgencyGlobalRankEntity entity = existingByAgencyId.get(agencyId);
            if (entity == null) {
                entity = new AgencyGlobalRankEntity();
                entity.setAgency(agencyById.get(agencyId));
            }

            entity.setRankNumber(assignment.getRankNumber());
            entity.setAssignedDate(assignedAt);
            entitiesToSave.add(entity);
        }

        List<AgencyGlobalRankEntity> entitiesToDelete = existingMappings.stream()
                .filter(existing -> !requestedAgencyIds.contains(existing.getAgency().getAgencyId()))
                .toList();
        if (!entitiesToDelete.isEmpty()) {
            agencyGlobalRankRepository.deleteAll(entitiesToDelete);
        }

        agencyGlobalRankRepository.saveAll(entitiesToSave);

        if (recruitmentNotificationId != null) {
            rankReleaseService.releaseEligibleRanksForNotification(recruitmentNotificationId);
        } else {
            rankReleaseService.releaseEligibleNotifications();
        }
    }

    private void validateInput(List<AgencyRankAssignmentCommand> assignments) {
        if (assignments == null || assignments.isEmpty()) {
            throw new RecruitmentNotificationException("At least one global agency rank assignment is required.");
        }

        Set<Long> uniqueAgencyIds = new HashSet<>();
        for (AgencyRankAssignmentCommand assignment : assignments) {
            if (assignment == null) {
                throw new RecruitmentNotificationException("Agency rank assignment row is invalid.");
            }
            if (assignment.getAgencyId() == null) {
                throw new RecruitmentNotificationException("Agency id is required in each rank assignment row.");
            }
            if (assignment.getRankNumber() == null || assignment.getRankNumber() < 1) {
                throw new RecruitmentNotificationException("Rank number must be at least 1.");
            }
            if (!uniqueAgencyIds.add(assignment.getAgencyId())) {
                throw new RecruitmentNotificationException(
                        "Duplicate agency in rank assignment: " + assignment.getAgencyId());
            }
        }
    }

    private Map<Long, AgencyGlobalRankEntity> mapByAgency(
            List<AgencyGlobalRankEntity> mappings) {
        Map<Long, AgencyGlobalRankEntity> mappingByAgency = new HashMap<>();
        for (AgencyGlobalRankEntity mapping : mappings) {
            mappingByAgency.put(mapping.getAgency().getAgencyId(), mapping);
        }
        return mappingByAgency;
    }

    private Map<Long, AgencyMaster> loadAndValidateAgencies(List<AgencyRankAssignmentCommand> assignments) {
        Set<Long> agencyIds = new HashSet<>();
        for (AgencyRankAssignmentCommand assignment : assignments) {
            agencyIds.add(assignment.getAgencyId());
        }

        Map<Long, AgencyMaster> agencyById = new HashMap<>();
        for (AgencyMaster agency : agencyRepository.findAllById(agencyIds)) {
            agencyById.put(agency.getAgencyId(), agency);
        }

        if (agencyById.size() != agencyIds.size()) {
            Set<Long> missing = new HashSet<>(agencyIds);
            missing.removeAll(agencyById.keySet());
            throw new RecruitmentNotificationException("Agency not found for ids: " + missing);
        }

        for (AgencyMaster agency : agencyById.values()) {
            if (agency.getStatus() != AgencyStatus.ACTIVE) {
                throw new RecruitmentNotificationException(
                        "Inactive agency cannot be assigned in global rank mapping: "
                                + agency.getAgencyName()
                                + " (id="
                                + agency.getAgencyId()
                                + ", status="
                                + agency.getStatus().name().toUpperCase(Locale.ROOT)
                                + ")");
            }
        }

        return agencyById;
    }
}
