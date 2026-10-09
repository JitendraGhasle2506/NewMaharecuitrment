package com.maharecruitment.gov.in.recruitment.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyLevelTwoAuthorityEntity;
import com.maharecruitment.gov.in.recruitment.entity.InternalVacancyOpeningEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoPanelMemberEntity;
import com.maharecruitment.gov.in.recruitment.entity.RecruitmentInternalLevelTwoScheduleEntity;
import com.maharecruitment.gov.in.recruitment.service.model.InternalVacancyLevelTwoWorkflowStatus;

final class InternalVacancyLevelTwoPanelMapper {

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "ROLE_STM", "STM",
            "ROLE_COO", "COO",
            "ROLE_HOD", "HOD");

    private InternalVacancyLevelTwoPanelMapper() {
    }

    static void replaceWithOpeningAuthorities(
            RecruitmentInternalLevelTwoScheduleEntity schedule,
            InternalVacancyOpeningEntity opening) {
        if (schedule == null) {
            return;
        }

        Map<Long, User> usersById = new LinkedHashMap<>();
        if (opening != null && opening.getLevelTwoInterviewAuthorities() != null) {
            opening.getLevelTwoInterviewAuthorities().stream()
                    .map(InternalVacancyLevelTwoAuthorityEntity::getUser)
                    .filter(user -> user != null && user.getId() != null)
                    .forEach(user -> usersById.putIfAbsent(user.getId(), user));
        }

        List<RecruitmentInternalLevelTwoPanelMemberEntity> panelMembers = usersById.values().stream()
                .map(InternalVacancyLevelTwoPanelMapper::toPanelMember)
                .toList();
        schedule.replacePanelMembers(panelMembers);
    }

    static void activateConfiguredPanel(
            RecruitmentInternalLevelTwoScheduleEntity schedule,
            Long assignedByUserId) {
        if (schedule == null
                || schedule.getInterviewDateTime() == null
                || schedule.getPanelMembers() == null
                || schedule.getPanelMembers().isEmpty()) {
            return;
        }

        schedule.setPanelAssignedByUserId(assignedByUserId);
        schedule.setPanelAssignedAt(LocalDateTime.now());
        if (schedule.getWorkflowStatus() != InternalVacancyLevelTwoWorkflowStatus.L2_RESCHEDULE_REQUESTED) {
            schedule.setWorkflowStatus(InternalVacancyLevelTwoWorkflowStatus.L2_PANEL_ASSIGNED);
        }
    }

    private static RecruitmentInternalLevelTwoPanelMemberEntity toPanelMember(User user) {
        RecruitmentInternalLevelTwoPanelMemberEntity member = new RecruitmentInternalLevelTwoPanelMemberEntity();
        member.setPanelUserId(user.getId());
        member.setPanelMemberName(resolveUserLabel(user));
        member.setPanelMemberDesignation(resolveRoleLabel(user));
        return member;
    }

    private static String resolveUserLabel(User user) {
        if (StringUtils.hasText(user.getName())) {
            return user.getName().trim();
        }
        if (StringUtils.hasText(user.getEmail())) {
            return user.getEmail().trim();
        }
        return "User " + user.getId();
    }

    private static String resolveRoleLabel(User user) {
        List<String> labels = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(role -> role != null && StringUtils.hasText(role.getName())
                                ? role.getName().trim().toUpperCase(Locale.ROOT)
                                : null)
                        .map(ROLE_LABELS::get)
                        .filter(StringUtils::hasText)
                        .distinct()
                        .toList();
        return labels.isEmpty() ? "Interview Authority" : String.join(", ", labels);
    }
}
