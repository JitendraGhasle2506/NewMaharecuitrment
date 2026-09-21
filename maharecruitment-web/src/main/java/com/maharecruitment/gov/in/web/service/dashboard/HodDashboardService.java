package com.maharecruitment.gov.in.web.service.dashboard;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HodDashboardService {
    private final ReportingManagerService reportingManagerService;
    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public List<EmployeeView> getEmployees(Long hodUserId) {
        if (hodUserId == null) {
            return List.of();
        }
        List<Long> employeeIds = reportingManagerService.getEffectiveEmployeeIdsForAuthority(hodUserId);
        if (employeeIds.isEmpty()) {
            return List.of();
        }
        return employeeRepository.findAllById(employeeIds).stream()
                .map(employee -> new EmployeeView(employee.getEmployeeCode(), employee.getFullName(),
                        employee.getDesignation() != null ? employee.getDesignation().getDesignationName() : null,
                        employee.getEmail(), employee.getRecruitmentType(), employee.getStatus()))
                .sorted(Comparator.comparing(EmployeeView::fullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public record EmployeeView(String employeeCode, String fullName, String designation, String email,
            String recruitmentType, String status) {
    }
}
