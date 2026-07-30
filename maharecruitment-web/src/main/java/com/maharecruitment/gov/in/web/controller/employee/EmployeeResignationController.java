package com.maharecruitment.gov.in.web.controller.employee;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;
import com.maharecruitment.gov.in.web.service.hr.EmployeeRelievingService;
import com.maharecruitment.gov.in.web.service.verification.AccountNotificationService;

@Controller
@RequestMapping("/employee/resignation")
@PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
public class EmployeeResignationController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRelievingService relievingService;
    private final AccountNotificationService accountNotificationService;
    private final UserRepository userRepository;

    public EmployeeResignationController(EmployeeRepository employeeRepository,
                                         EmployeeRelievingService relievingService,
                                         AccountNotificationService accountNotificationService,
                                         UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.relievingService = relievingService;
        this.accountNotificationService = accountNotificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String viewResignationForm(Principal principal, Model model) {
        String email = principal.getName();
        Optional<EmployeeEntity> profile = resolveCurrentEmployee(email);
        if (profile.isEmpty()) {
            model.addAttribute("errorMessage", "Employee profile not found.");
            return "employee/profile-unavailable";
        }
        
        EmployeeEntity currentEmployee = profile.get();
        model.addAttribute("employee", currentEmployee);
        
        List<EmployeeRelievingDto> records = relievingService.getAllRelievingRecords();
        boolean hasActive = records.stream()
            .anyMatch(r -> r.getEmployeeId().equals(currentEmployee.getEmployeeId()) &&
                           r.getStatus() != null && !r.getStatus().equalsIgnoreCase("Cancelled"));
        
        model.addAttribute("hasActiveResignation", hasActive);
        
        if (!hasActive) {
            model.addAttribute("relievingDto", new EmployeeRelievingDto());
        }

        return "employee/resignation";
    }

    @PostMapping("/submit")
    public String submitResignation(Principal principal,
                                    @org.springframework.web.bind.annotation.ModelAttribute("relievingDto") EmployeeRelievingDto dto,
                                    RedirectAttributes redirectAttributes) {
        String email = principal.getName();
        Optional<EmployeeEntity> profile = resolveCurrentEmployee(email);
        if (profile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Employee profile not found.");
            return "redirect:/employee/dashboard";
        }
        EmployeeEntity currentEmployee = profile.get();

        try {
            dto.setEmployeeId(currentEmployee.getEmployeeId());
            dto.setReasonOfRelieving("Resignation");
            dto.setStatus("Resignation Submitted");
            
            relievingService.saveRelieving(dto);
            
            List<User> hrUsers = userRepository.findDistinctUsersByRoleNames(List.of("ROLE_HR"));
            for (User hr : hrUsers) {
                if (hr.getEmail() != null && !hr.getEmail().isBlank()) {
                    accountNotificationService.sendResignationNotification(hr.getEmail(), "HR", currentEmployee.getFullName(), dto.getResignDate() != null ? dto.getResignDate().toString() : "Not specified");
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Resignation submitted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting resignation: " + e.getMessage());
        }

        return "redirect:/employee/resignation";
    }

    private Optional<EmployeeEntity> resolveCurrentEmployee(String email) {
        return userRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .flatMap(user -> employeeRepository.findDetailedByUserId(user.getId()));
    }
}
