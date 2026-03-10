package com.maharecruitment.gov.in.department.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.HrDepartmentRequestService;

@Controller
@RequestMapping("/hr/department-requests")
public class HrDepartmentRequestController {

    private static final Logger log = LoggerFactory.getLogger(HrDepartmentRequestController.class);

    private final HrDepartmentRequestService hrDepartmentRequestService;

    public HrDepartmentRequestController(HrDepartmentRequestService hrDepartmentRequestService) {
        this.hrDepartmentRequestService = hrDepartmentRequestService;
    }

    @GetMapping
    public String departmentRequestSummary(Model model) {
        try {
            model.addAttribute("parentDepartmentRequests", hrDepartmentRequestService.getParentDepartmentRequests());
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load HR parent department requests. reason={}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("parentDepartmentRequests", List.of());
        } catch (Exception ex) {
            log.error("Unexpected error while loading HR parent department requests.", ex);
            model.addAttribute(
                    "errorMessage",
                    "Unable to load parent departments right now. Please try again.");
            model.addAttribute("parentDepartmentRequests", List.of());
        }
        return "hr/department-request-list";
    }

    @GetMapping("/{departmentId}/subdepartments")
    public String subDepartmentProjectCounts(
            @PathVariable Long departmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute(
                    "subDepartmentRequest",
                    hrDepartmentRequestService.getSubDepartmentProjectCounts(departmentId));
            return "hr/department-request-subdepartment-list";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load sub-department request counts for departmentId={}, reason={}",
                    departmentId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/department-requests";
        }
    }

    @GetMapping("/{departmentId}/subdepartments/{subDepartmentId}/applications")
    public String submittedApplications(
            @PathVariable Long departmentId,
            @PathVariable Long subDepartmentId,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute(
                    "applicationDetail",
                    hrDepartmentRequestService.getSubDepartmentApplications(departmentId, subDepartmentId));
            return "hr/department-request-applications";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load submitted applications for departmentId={}, subDepartmentId={}, reason={}",
                    departmentId,
                    subDepartmentId,
                    ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/hr/department-requests/" + departmentId + "/subdepartments";
        }
    }
}
