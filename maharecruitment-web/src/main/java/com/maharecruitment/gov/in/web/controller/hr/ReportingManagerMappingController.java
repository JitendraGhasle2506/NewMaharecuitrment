package com.maharecruitment.gov.in.web.controller.hr;

import java.util.List;
import java.util.Map;

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

    @Autowired
    private ReportingManagerService reportingManagerService;

    @GetMapping("/reportingManager")
    public String reportingManagerView(Model model) {
        model.addAttribute("sidebarActive", "Reporting Manager");
        return "hr/reporting-manager-mapping";
    }

    @GetMapping("/api/hods")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getHods() {
        return ResponseEntity.ok(reportingManagerService.getHodUsers());
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
    public ResponseEntity<List<Map<String, Object>>> getInternalEmployees(@RequestParam(required = false) Long includeEmployeeId) {
        return ResponseEntity.ok(reportingManagerService.getInternalEmployees(includeEmployeeId));
    }

    @GetMapping("/api/mappings")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getAllMappings() {
        return ResponseEntity.ok(reportingManagerService.getAllMappings());
    }

    @PostMapping("/saveReportingMapping")
    public String saveReportingMapping(
            @RequestParam Long hodUserId,
            @RequestParam String managerType,
            @RequestParam Long managerEmployeeId,
            @RequestParam Long projectId,
            @RequestParam List<Long> employeeIds,
            RedirectAttributes redirectAttributes) {

        try {
            reportingManagerService.saveMapping(hodUserId, managerType, managerEmployeeId, projectId, employeeIds);
            redirectAttributes.addFlashAttribute("successMessage", "Reporting managers mapped successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error mapping reporting managers: " + e.getMessage());
        }

        return "redirect:/hr/reportingManager";
    }
}
