package com.maharecruitment.gov.in.web.controller.hr;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeRelievingService;

@Controller
@RequestMapping("/hr/relieving")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeRelievingController {

    private final EmployeeRelievingService relievingService;
    private final EmployeeRepository employeeRepository;

    public EmployeeRelievingController(EmployeeRelievingService relievingService,
                                       EmployeeRepository employeeRepository) {
        this.relievingService = relievingService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping
    public String viewRelievingForm(Model model) {
        List<EmployeeRelievingDto> allRecords = relievingService.getAllRelievingRecords();
        
        // Filter out employees who already have an active relieving record
        java.util.Set<Long> employeesWithActiveRelieving = allRecords.stream()
                .filter(r -> r.getStatus() != null && !r.getStatus().equalsIgnoreCase("Cancelled"))
                .map(EmployeeRelievingDto::getEmployeeId)
                .collect(java.util.stream.Collectors.toSet());

        // Only show ACTIVE internal employees in both dropdowns who don't have pending relieving processes
        List<EmployeeEntity> internalActiveEmployees =
                employeeRepository.findByRecruitmentTypeAndStatus("INTERNAL", "ACTIVE").stream()
                .filter(e -> !employeesWithActiveRelieving.contains(e.getEmployeeId()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("employees", internalActiveEmployees);
        model.addAttribute("relievingDto", new EmployeeRelievingDto());
        model.addAttribute("allRecords", allRecords);

        return "hr/employee-relieving";
    }

    @PostMapping("/save")
    public String saveRelievingForm(@ModelAttribute("relievingDto") EmployeeRelievingDto dto,
                                    RedirectAttributes redirectAttributes) {
        try {
            if (dto.getEmployeeId() == null) {
                throw new IllegalArgumentException("Please select an employee.");
            }
            if (dto.getReasonOfRelieving() == null || dto.getReasonOfRelieving().isBlank()) {
                throw new IllegalArgumentException("Reason of relieving is required.");
            }
            if ("Resignation".equalsIgnoreCase(dto.getReasonOfRelieving())) {
                if (dto.getResignDate() == null) {
                    throw new IllegalArgumentException("Resign date is required.");
                }
                dto.setStatus("Resignation Submitted");
            } else if ("PIP".equalsIgnoreCase(dto.getReasonOfRelieving())) {
                if (dto.getPipStartDate() == null) {
                    throw new IllegalArgumentException("PIP Start Date is required.");
                }
                if (dto.getPipDuration() == null || dto.getPipDuration().isBlank()) {
                    throw new IllegalArgumentException("PIP Duration is required.");
                }
                dto.setStatus("Under PIP");
            } else {
                if (dto.getExitDate() == null) {
                    throw new IllegalArgumentException("Exit date is required.");
                }
            }
            relievingService.saveRelieving(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee relieving record saved successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error saving record: " + e.getMessage());
        }
        return "redirect:/hr/relieving";
    }

    @PostMapping("/mark-exit-date")
    public String markExitDate(@ModelAttribute("relievingDto") EmployeeRelievingDto dto,
                               RedirectAttributes redirectAttributes) {
        try {
            if (dto.getRelievingId() == null || dto.getExitDate() == null) {
                throw new IllegalArgumentException("Relieving ID and Exit Date are required.");
            }
            relievingService.markExitDate(dto.getRelievingId(), dto.getExitDate());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Exit Date marked successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Error marking exit date: " + e.getMessage());
        }
        return "redirect:/hr/relieving";
    }

    @PostMapping("/cancel-record")
    public String cancelRecord(@org.springframework.web.bind.annotation.RequestParam("relievingId") Long relievingId, RedirectAttributes redirectAttributes) {
        try {
            relievingService.cancelResignation(relievingId);
            redirectAttributes.addFlashAttribute("successMessage", "Process cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling process: " + e.getMessage());
        }
        return "redirect:/hr/relieving";
    }

    @PostMapping("/edit-pip")
    public String editPip(@org.springframework.web.bind.annotation.RequestParam("relievingId") Long relievingId,
                          @org.springframework.web.bind.annotation.RequestParam("pipStartDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate pipStartDate,
                          @org.springframework.web.bind.annotation.RequestParam("pipDuration") String pipDuration,
                          RedirectAttributes redirectAttributes) {
        try {
            relievingService.updatePip(relievingId, pipStartDate, pipDuration);
            redirectAttributes.addFlashAttribute("successMessage", "PIP details updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating PIP details: " + e.getMessage());
        }
        return "redirect:/hr/relieving";
    }

    @GetMapping("/employee-details/{id}")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, String> getEmployeeDetails(@org.springframework.web.bind.annotation.PathVariable Long id) {
        EmployeeEntity emp = employeeRepository.findById(id).orElse(null);
        java.util.Map<String, String> details = new java.util.HashMap<>();
        if (emp != null) {
            String agencyName = (emp.getAgency() != null) ? emp.getAgency().getAgencyName() : "N/A";
            String projectName = "N/A";
            if (emp.getPreOnboarding() != null
                    && emp.getPreOnboarding().getInterviewDetail() != null
                    && emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification() != null
                    && emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst() != null
                    && org.springframework.util.StringUtils.hasText(emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification()
                            .getProjectMst().getProjectName())) {
                projectName = emp.getPreOnboarding().getInterviewDetail().getRecruitmentNotification().getProjectMst()
                        .getProjectName();
            }
            details.put("agencyName", agencyName);
            details.put("projectName", projectName);
        } else {
            details.put("agencyName", "N/A");
            details.put("projectName", "N/A");
        }
        return details;
    }
}
