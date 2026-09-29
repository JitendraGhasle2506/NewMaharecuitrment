package com.maharecruitment.gov.in.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeHierarchyNode;
import com.maharecruitment.gov.in.recruitment.dto.organization.EmployeeReportingType;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.recruitment.service.organization.EmployeeHierarchyService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_HOD', 'ROLE_STM', 'ROLE_EMPLOYEE')")
public class HodEmployeeHierarchyController {
    private final EmployeeHierarchyService hierarchyService;
    private final EmployeeRepository employeeRepository;

    public record Result<T>(boolean success, String message, T data) {
        private static <T> Result<T> of(T data) {
            return new Result<>(true, "Employee hierarchy fetched successfully", data);
        }
    }

    @GetMapping("/employee/api/employee-hierarchy/reporting-chain")
    public Result<List<EmployeeHierarchyService.ReportingChainMember>> reportingChain(HttpSession session) {
        return Result.of(hierarchyService.reportingChain(currentHod(session).getEmployeeId(),
                EmployeeReportingType.PRIMARY));
    }

    @GetMapping({"/hod1/api/employee-hierarchy/options", "/employee/api/employee-hierarchy/options"})
    public Result<EmployeeHierarchyService.Options> options(HttpSession session) {
        EmployeeEntity hod = currentHod(session);
        String code = hod.getEmployeeCode();
        String label = hod.getFullName()
                + (code == null || code.isBlank() ? "" : " (" + code.trim() + ")");
        var option = new EmployeeHierarchyService.Option(hod.getEmployeeId(), label);
        return Result.of(new EmployeeHierarchyService.Options(List.of(option), List.of(), List.of()));
    }

    @GetMapping({"/hod1/api/employee-hierarchy/{rootEmployeeId}", "/employee/api/employee-hierarchy/{rootEmployeeId}"})
    public Result<EmployeeHierarchyNode> tree(
            @PathVariable Long rootEmployeeId,
            @RequestParam(defaultValue = "PRIMARY") EmployeeReportingType reportingType,
            @RequestParam(required = false) Long nodeId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "25") int limit,
            @RequestParam(defaultValue = "1") int depth,
            HttpSession session) {
        Long hodEmployeeId = requireOwnRoot(rootEmployeeId, session);
        return Result.of(hierarchyService.tree(
                hodEmployeeId, nodeId, reportingType, null, null, offset, limit, depth));
    }

    @GetMapping({"/hod1/api/employee-hierarchy/{rootEmployeeId}/search", "/employee/api/employee-hierarchy/{rootEmployeeId}/search"})
    public Result<List<EmployeeHierarchyService.SearchMatch>> search(
            @PathVariable Long rootEmployeeId,
            @RequestParam(defaultValue = "PRIMARY") EmployeeReportingType reportingType,
            @RequestParam String q,
            HttpSession session) {
        Long hodEmployeeId = requireOwnRoot(rootEmployeeId, session);
        return Result.of(hierarchyService.search(hodEmployeeId, reportingType, null, null, q));
    }

    private Long requireOwnRoot(Long requestedRootId, HttpSession session) {
        Long hodEmployeeId = currentHod(session).getEmployeeId();
        if (!hodEmployeeId.equals(requestedRootId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can view only your own reporting hierarchy.");
        }
        return hodEmployeeId;
    }

    private EmployeeEntity currentHod(HttpSession session) {
        Object candidate = session.getAttribute("SESSION_USER");
        if (!(candidate instanceof SessionUserDTO user) || user.id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session has expired.");
        }
        return employeeRepository.findByUser_Id(user.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Your user account is not linked to an active employee record."));
    }
}
