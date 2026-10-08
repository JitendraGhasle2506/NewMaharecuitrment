package com.maharecruitment.gov.in.attendance.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.attendance.repository.ApprovalRecruitmentTypeRuleRepository;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeReportingMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeReportingMappingRepository;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class RecruitmentTypeApprovalRoutingService {

    public static final String STAGE_MANAGER = "MANAGER";
    public static final String STAGE_HOD = "HOD";

    private final ApprovalRecruitmentTypeRuleRepository ruleRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeReportingMappingRepository mappingRepository;

    public RecruitmentTypeApprovalRoutingService(
            ApprovalRecruitmentTypeRuleRepository ruleRepository,
            EmployeeRepository employeeRepository,
            EmployeeReportingMappingRepository mappingRepository) {
        this.ruleRepository = ruleRepository;
        this.employeeRepository = employeeRepository;
        this.mappingRepository = mappingRepository;
    }

    public ApprovalRoute routeNewRequest(Long employeeId) {
        if (employeeId == null || !ruleRepository.isEnabledForEmployee(employeeId)) {
            return ApprovalRoute.legacy();
        }

        EmployeeReportingMappingEntity mapping = mappingRepository
                .findFirstByEmployeeIdOrderByMappingIdDesc(employeeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reporting manager and HOD must be configured before applying for leave or tour."));
        Long hodUserId = mapping.getHodUserId();
        if (hodUserId == null) {
            throw new IllegalArgumentException("Reporting HOD must be configured before applying for leave or tour.");
        }

        Long managerUserId = resolveManagerUserId(mapping.getManagerEmployeeId());
        String initialStage = managerUserId == null || Objects.equals(managerUserId, hodUserId)
                ? STAGE_HOD
                : STAGE_MANAGER;
        return new ApprovalRoute(initialStage, managerUserId, hodUserId);
    }

    public boolean canAct(
            Long actorUserId,
            String approvalStage,
            Long managerApproverUserId,
            Long hodApproverUserId) {
        if (STAGE_MANAGER.equalsIgnoreCase(approvalStage)) {
            return Objects.equals(actorUserId, managerApproverUserId);
        }
        if (STAGE_HOD.equalsIgnoreCase(approvalStage)) {
            return Objects.equals(actorUserId, hodApproverUserId);
        }
        return false;
    }

    public boolean isTwoLevelStage(String approvalStage) {
        return STAGE_MANAGER.equalsIgnoreCase(approvalStage)
                || STAGE_HOD.equalsIgnoreCase(approvalStage);
    }

    private Long resolveManagerUserId(Long managerEmployeeId) {
        if (managerEmployeeId == null) {
            return null;
        }
        return employeeRepository.findById(managerEmployeeId)
                .map(EmployeeEntity::getUser)
                .map(User::getId)
                .orElse(null);
    }

    public record ApprovalRoute(String initialStage, Long managerUserId, Long hodUserId) {

        public static ApprovalRoute legacy() {
            return new ApprovalRoute(null, null, null);
        }

        public boolean applies() {
            return initialStage != null;
        }
    }
}
