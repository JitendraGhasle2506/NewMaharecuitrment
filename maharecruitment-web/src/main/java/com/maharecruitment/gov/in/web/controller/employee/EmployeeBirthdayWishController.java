package com.maharecruitment.gov.in.web.controller.employee;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.web.service.employee.EmployeeBirthdayWishService;

@Controller
@RequestMapping("/employee/birthday-wishes")
@PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
public class EmployeeBirthdayWishController {

    private final EmployeeBirthdayWishService birthdayWishService;

    public EmployeeBirthdayWishController(EmployeeBirthdayWishService birthdayWishService) {
        this.birthdayWishService = birthdayWishService;
    }

    @PostMapping
    public String sendWish(
            Principal principal,
            @RequestParam("recipientEmployeeId") Long recipientEmployeeId,
            @RequestParam("message") String message,
            RedirectAttributes redirectAttributes) {
        try {
            birthdayWishService.sendWish(loginEmail(principal), recipientEmployeeId, message);
            redirectAttributes.addFlashAttribute("birthdayWishSuccess", "Birthday wish sent successfully.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("birthdayWishError", ex.getMessage());
        }
        return "redirect:/employee/dashboard";
    }

    @PostMapping("/{wishId}/reply")
    public String replyToWish(
            Principal principal,
            @PathVariable Long wishId,
            @RequestParam("reply") String reply,
            RedirectAttributes redirectAttributes) {
        try {
            birthdayWishService.replyToWish(loginEmail(principal), wishId, reply);
            redirectAttributes.addFlashAttribute("birthdayWishSuccess", "Reply saved successfully.");
        } catch (RecruitmentNotificationException ex) {
            redirectAttributes.addFlashAttribute("birthdayWishError", ex.getMessage());
        }
        return "redirect:/employee/dashboard";
    }

    private String loginEmail(Principal principal) {
        return principal == null ? null : principal.getName();
    }
}
