package com.maharecruitment.gov.in.web.controller.hr;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.service.ReportingManagerService;

@Controller
@RequestMapping("/hr")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class ReportingManagerMappingController {

    private static final Logger log = LoggerFactory.getLogger(ReportingManagerMappingController.class);

    @Autowired
    private ReportingManagerService reportingManagerService;

    @GetMapping({ "/reportingManager", "/reportingManagers" })
    public String reportingManagerView(Model model) {
        model.addAttribute("sidebarActive", "Reporting Manager");
        return "hr/reporting-manager-mapping";
    }

    @GetMapping("/api/hods")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getHods() {
        return ResponseEntity.ok(reportingManagerService.getHodUsers());
    }

    @GetMapping("/api/reporting-authorities")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getReportingAuthorities() {
        return ResponseEntity.ok(reportingManagerService.getReportingAuthorities());
    }

    @GetMapping("/api/cell-authority-users")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCellAuthorityUsers() {
        return ResponseEntity.ok(reportingManagerService.getCellAuthorityUsers());
    }

    @GetMapping("/api/managers")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getManagers(@RequestParam String type) {
        return ResponseEntity.ok(reportingManagerService.getManagersByType(type));
    }

    @GetMapping("/api/projects")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getProjects() {
        return ResponseEntity.ok(reportingManagerService.getProjects());
    }

    @GetMapping("/api/employees")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getInternalEmployees(
            @RequestParam(required = false) Long includeEmployeeId,
            @RequestParam(required = false) Long hodUserId,
            @RequestParam(required = false) String managerType,
            @RequestParam(required = false) Long managerEmployeeId) {
        return ResponseEntity.ok(
                reportingManagerService.getInternalEmployees(
                        includeEmployeeId, hodUserId, managerType, managerEmployeeId));
    }

    @GetMapping("/api/mappings")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllMappings() {
        return ResponseEntity.ok(reportingManagerService.getAllMappings());
    }

    @GetMapping("/api/reporting-assignments")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getEmployeeReportingAssignments() {
        return ResponseEntity.ok(reportingManagerService.getEmployeeReportingAssignments());
    }

    @GetMapping("/api/cell-reporting-mappings")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getCellReportingMappings() {
        return ResponseEntity.ok(reportingManagerService.getCellReportingMappings());
    }

    @PostMapping("/saveCellReportingMapping")
    public String saveCellReportingMapping(
            @RequestParam(required = false) Long mappingId,
            @RequestParam Long cellId,
            @RequestParam(defaultValue = "1") Integer authorityLevel,
            @RequestParam Long authorityUserId,
            RedirectAttributes redirectAttributes) {
        try {
            reportingManagerService.saveCellReportingMapping(
                    mappingId, cellId, authorityLevel, authorityUserId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Cell Level " + authorityLevel + " reporting authority mapped successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Cell reporting mapping validation failed for cellId={}, authorityLevel={}, authorityUserId={}: {}",
                    cellId, authorityLevel, authorityUserId, e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Error mapping cell reporting authority: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected cell reporting mapping failure for cellId={}, authorityLevel={}, authorityUserId={}",
                    cellId, authorityLevel, authorityUserId, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to save the cell reporting authority. Please try again.");
        }
        return "redirect:/hr/reportingManagers";
    }

    @PostMapping("/saveReportingMapping")
    public String saveReportingMapping(
            @RequestParam Long hodUserId,
            @RequestParam(required = false) String managerType,
            @RequestParam(required = false) Long managerEmployeeId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long mappingId,
            @RequestParam List<Long> employeeIds,
            RedirectAttributes redirectAttributes) {

        try {
            if (mappingId == null) {
                reportingManagerService.saveMapping(
                        hodUserId, managerType, managerEmployeeId, projectId, employeeIds);
                redirectAttributes.addFlashAttribute(
                        "successMessage", "Reporting managers mapped successfully.");
            } else {
                if (employeeIds.size() != 1) {
                    throw new IllegalArgumentException("Select exactly one employee when editing a mapping.");
                }
                reportingManagerService.updateMapping(
                        mappingId, hodUserId, managerType, managerEmployeeId, projectId, employeeIds.get(0));
                redirectAttributes.addFlashAttribute(
                        "successMessage", "Reporting manager mapping updated successfully.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Reporting mapping validation failed for hodUserId={} and managerType={}: {}",
                    hodUserId, managerType, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Error mapping reporting managers: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected reporting mapping failure for hodUserId={} and managerType={}",
                    hodUserId, managerType, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to save reporting manager mapping. Please try again.");
        }

        return "redirect:/hr/reportingManagers";
    }

    @PostMapping("/changeReportingAuthority")
    public String changeReportingAuthority(
            @RequestParam Long employeeId,
            @RequestParam Long authorityUserId,
            @RequestParam(required = false) String managerType,
            @RequestParam(required = false) Long managerEmployeeId,
            RedirectAttributes redirectAttributes) {
        try {
            reportingManagerService.changeReportingAuthority(
                    employeeId, authorityUserId, managerType, managerEmployeeId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "Employee reporting assignment changed successfully.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Reporting authority change failed for employeeId={} and authorityUserId={}: {}",
                    employeeId, authorityUserId, e.getMessage());
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to change reporting authority: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected reporting authority change failure for employeeId={} and authorityUserId={}",
                    employeeId, authorityUserId, e);
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Unable to change the reporting authority. Please try again.");
        }
        return "redirect:/hr/reportingManagers";
    }
}
