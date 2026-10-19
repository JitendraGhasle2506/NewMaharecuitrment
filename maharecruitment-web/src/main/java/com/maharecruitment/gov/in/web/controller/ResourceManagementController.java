package com.maharecruitment.gov.in.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceManagementService;
import com.maharecruitment.gov.in.recruitment.service.organization.ResourceManagementService.EmployeeTypeFilter;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/reporting-manager/resources")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE', 'ROLE_HOD', 'ROLE_STM', 'ROLE_PM', 'ROLE_COO', 'ROLE_CTO', 'ROLE_CFO', 'ROLE_MD')")
public class ResourceManagementController {
    private final ResourceManagementService service;

    public record Assignment(@NotNull @Positive Long managerEmployeeId) { }
    public record Message(boolean success, String message) { }

    @GetMapping
    public String page(HttpSession session) {
        currentUserId(session);
        return "employee/resource-management";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResourceManagementService.Page list(
            @RequestParam(defaultValue = "mapped") String view,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "INTERNAL_MAHAIT") EmployeeTypeFilter employeeType,
            HttpSession session) {
        return service.list(currentUserId(session), view, q, page, size, employeeType);
    }

    @PostMapping("/api/{employeeId}/map")
    @ResponseBody
    public Message map(@PathVariable Long employeeId, @Valid @RequestBody Assignment assignment, HttpSession session) {
        service.map(currentUserId(session), employeeId, assignment.managerEmployeeId());
        return new Message(true, "Resource mapped successfully.");
    }

    @PostMapping("/api/{employeeId}/reassign")
    @ResponseBody
    public Message reassign(@PathVariable Long employeeId, @Valid @RequestBody Assignment assignment, HttpSession session) {
        service.reassign(currentUserId(session), employeeId, assignment.managerEmployeeId());
        return new Message(true, "Reporting manager updated successfully.");
    }

    @PostMapping("/api/{employeeId}/release")
    @ResponseBody
    public Message release(@PathVariable Long employeeId, HttpSession session) {
        service.release(currentUserId(session), employeeId);
        return new Message(true, "Primary reporting assignment released. Any other reporting assignments remain in place.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<Message> handleStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new Message(false, exception.getReason()));
    }

    private Long currentUserId(HttpSession session) {
        if (!(session.getAttribute("SESSION_USER") instanceof SessionUserDTO user) || user.id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Your session has expired. Please sign in again.");
        }
        return user.id();
    }
}
