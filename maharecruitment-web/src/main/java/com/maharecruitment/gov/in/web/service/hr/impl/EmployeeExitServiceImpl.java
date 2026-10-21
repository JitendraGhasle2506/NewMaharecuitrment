package com.maharecruitment.gov.in.web.service.hr.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.maharecruitment.gov.in.audit.dto.AuditRecordRequest;
import com.maharecruitment.gov.in.audit.service.AuditTrailService;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.repository.RecruitmentDesignationVacancyRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeExitService;

@Service
public class EmployeeExitServiceImpl implements EmployeeExitService {
    private final EmployeeRepository employees;
    private final UserRepository users;
    private final RecruitmentDesignationVacancyRepository vacancies;
    private final AuditTrailService audit;

    public EmployeeExitServiceImpl(EmployeeRepository employees, UserRepository users,
            RecruitmentDesignationVacancyRepository vacancies, AuditTrailService audit) {
        this.employees = employees;
        this.users = users;
        this.vacancies = vacancies;
        this.audit = audit;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_HR')")
    @Transactional
    public void exitEmployee(Long employeeId, LocalDate lastWorkingDate, String reason, String actorLogin) {
        if (employeeId == null || employeeId < 1) throw invalid("Employee id is required.");
        if (lastWorkingDate == null || lastWorkingDate.isAfter(LocalDate.now(ZoneId.of("Asia/Kolkata")))) {
            throw invalid("Last working date is required and cannot be in the future.");
        }
        if (!StringUtils.hasText(reason) || reason.length() > 1000) {
            throw invalid("Enter an exit reason of at most 1,000 characters.");
        }
        if (!StringUtils.hasText(actorLogin)) throw invalid("An authenticated HR user is required.");
        var actor = users.findByEmailIgnoreCaseAndActiveTrue(actorLogin)
                .orElseThrow(() -> invalid("An active HR account is required."));
        EmployeeEntity employee = employees.lockForExit(employeeId)
                .orElseThrow(() -> invalid("Employee not found."));
        var user = employee.getUser();
        if ("RESIGNED".equalsIgnoreCase(employee.getStatus())
                && (user == null || !Boolean.TRUE.equals(user.getActive()))) {
            throw invalid("This employee has already exited and their access is disabled.");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("previousStatus", employee.getStatus());
        metadata.put("previousResignationDate", employee.getResignationDate());
        metadata.put("employeeCode", employee.getEmployeeCode());
        metadata.put("linkedUserId", user == null ? null : user.getId());
        metadata.put("previousAccountActive", user == null ? null : user.getActive());
        metadata.put("vacancyReleased", releaseMappedVacancy(employee));
        employee.setStatus("RESIGNED");
        employee.setResignationDate(lastWorkingDate);
        if (user != null) {
            user.setActive(false);
            users.save(user);
        }
        employees.save(employee);
        metadata.put("newStatus", "RESIGNED");
        metadata.put("lastWorkingDate", lastWorkingDate.toString());
        metadata.put("reason", reason.trim());
        metadata.put("accountActive", false);
        audit.record(AuditRecordRequest.builder().moduleName("EMPLOYEE_EXIT").entityType("EMPLOYEE")
                .entityId(employeeId.toString()).actionType("HR_EMPLOYEE_EXITED")
                .actorUserId(actor.getId()).actorLoginId(actor.getEmail()).actorName(actor.getName())
                .activitySummary("HR recorded employee exit and disabled system access")
                .metadata(metadata).build());
    }

    private boolean releaseMappedVacancy(EmployeeEntity employee) {
        // Imported employees can exit without an onboarding record or a vacancy mapping.
        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus()) || employee.getPreOnboarding() == null
                || employee.getPreOnboarding().getInterviewDetail() == null) return false;
        var interview = employee.getPreOnboarding().getInterviewDetail();
        if (interview.getDesignationVacancy() == null || interview.getRecruitmentNotification() == null) return false;
        if (interview.getDesignationVacancy().getRecruitmentDesignationVacancyId() == null
                || interview.getRecruitmentNotification().getRecruitmentNotificationId() == null) return false;
        var vacancy = vacancies.findByIdForFinalDecisionUpdate(
                interview.getDesignationVacancy().getRecruitmentDesignationVacancyId(),
                interview.getRecruitmentNotification().getRecruitmentNotificationId());
        if (vacancy.isEmpty() || vacancy.get().getFillPost() == null || vacancy.get().getFillPost() <= 0) return false;
        vacancy.get().setFillPost(vacancy.get().getFillPost() - 1);
        vacancies.save(vacancy.get());
        return true;
    }

    private RecruitmentNotificationException invalid(String message) {
        return new RecruitmentNotificationException(message);
    }
}
