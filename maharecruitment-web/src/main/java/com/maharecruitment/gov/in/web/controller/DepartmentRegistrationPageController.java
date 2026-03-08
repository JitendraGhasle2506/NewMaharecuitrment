package com.maharecruitment.gov.in.web.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.master.dto.DepartmentResponse;
import com.maharecruitment.gov.in.master.dto.SubDepartmentResponse;
import com.maharecruitment.gov.in.master.service.DepartmentMstService;
import com.maharecruitment.gov.in.master.service.SubDepartmentService;
import com.maharecruitment.gov.in.web.dto.registration.DepartmentRegistrationForm;
import com.maharecruitment.gov.in.web.dto.verification.VerificationChannel;
import com.maharecruitment.gov.in.web.service.registration.DepartmentRegistrationPageService;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/register")
public class DepartmentRegistrationPageController {

    private final DepartmentMstService departmentService;
    private final SubDepartmentService subDepartmentService;
    private final DepartmentRegistrationPageService registrationPageService;
    private final OtpVerificationService otpVerificationService;

    public DepartmentRegistrationPageController(
            DepartmentMstService departmentService,
            SubDepartmentService subDepartmentService,
            DepartmentRegistrationPageService registrationPageService,
            OtpVerificationService otpVerificationService) {
        this.departmentService = departmentService;
        this.subDepartmentService = subDepartmentService;
        this.registrationPageService = registrationPageService;
        this.otpVerificationService = otpVerificationService;
    }

    @GetMapping("/department-registration")
    public String registrationPage(Model model, HttpSession session) {
        populateForm(model, new DepartmentRegistrationForm(), session);
        return "register/department-registration";
    }

    @PostMapping("/department-registration")
    public String register(
            @Valid @ModelAttribute("registrationForm") DepartmentRegistrationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        validateDynamicSelections(form, bindingResult, session);

        if (bindingResult.hasErrors()) {
            populateForm(model, form, session);
            return "register/department-registration";
        }

        try {
            registrationPageService.register(form);
            otpVerificationService.clear(session, VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT);
            redirectAttributes.addAttribute("registered", "true");
            return "redirect:/login";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateForm(model, form, session);
            return "register/department-registration";
        }
    }

    @GetMapping("/sub-departments")
    @ResponseBody
    public List<SubDepartmentResponse> getSubDepartments(@RequestParam Long departmentId) {
        return subDepartmentService.getAll(departmentId, Pageable.unpaged()).getContent();
    }

    private void populateForm(Model model, DepartmentRegistrationForm form, HttpSession session) {
        model.addAttribute("registrationForm", form);
        model.addAttribute("departments", getDepartments());
        model.addAttribute("subDepartments", getSubDepartmentsForForm(form));
        model.addAttribute("primaryMobileVerified",
                otpVerificationService.isVerified(
                        session,
                        VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT,
                        VerificationChannel.MOBILE,
                        form.getPrimaryMobile()));
        model.addAttribute("primaryEmailVerified",
                otpVerificationService.isVerified(
                        session,
                        VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT,
                        VerificationChannel.EMAIL,
                        form.getPrimaryEmail()));
        model.addAttribute("verificationPurpose", VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT);
    }

    private List<DepartmentResponse> getDepartments() {
        return departmentService.getAll(Pageable.unpaged()).getContent();
    }

    private List<SubDepartmentResponse> getSubDepartmentsForForm(DepartmentRegistrationForm form) {
        if (form.getDepartmentId() == null || form.isOtherDepartmentSelected()) {
            return Collections.emptyList();
        }
        return subDepartmentService.getAll(form.getDepartmentId(), Pageable.unpaged()).getContent();
    }

    private void validateDynamicSelections(
            DepartmentRegistrationForm form,
            BindingResult bindingResult,
            HttpSession session) {
        if (form.isOtherDepartmentSelected()) {
            if (isBlank(form.getNewDepartmentName())) {
                bindingResult.rejectValue("newDepartmentName", "registration.newDepartmentName",
                        "New department name is required.");
            }
            if (isBlank(form.getNewSubDeptName())) {
                bindingResult.rejectValue("newSubDeptName", "registration.newSubDeptName",
                        "New sub-department name is required.");
            }
        } else {
            if (form.getSubDeptId() == null) {
                bindingResult.rejectValue("subDeptId", "registration.subDeptId",
                        "Sub-department selection is required.");
            } else if (form.isOtherSubDepartmentSelected() && isBlank(form.getNewSubDeptName())) {
                bindingResult.rejectValue("newSubDeptName", "registration.newSubDeptName",
                        "New sub-department name is required.");
            }
        }

        if (form.getPanFile() == null || form.getPanFile().isEmpty()) {
            bindingResult.rejectValue("panFile", "registration.panFile", "PAN document is required.");
        }

        if (!isBlank(form.getPrimaryEmail())
                && form.getPrimaryEmail().trim().equalsIgnoreCase(form.getSecondaryEmail())) {
            bindingResult.rejectValue("secondaryEmail", "registration.secondaryEmail",
                    "Secondary email must be different from primary email.");
        }

        if (!isBlank(form.getPrimaryMobile()) && form.getPrimaryMobile().equals(form.getSecondaryMobile())) {
            bindingResult.rejectValue("secondaryMobile", "registration.secondaryMobile",
                    "Secondary mobile must be different from primary mobile.");
        }

        if (!bindingResult.hasFieldErrors("primaryMobile")
                && !otpVerificationService.isVerified(
                        session,
                        VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT,
                        VerificationChannel.MOBILE,
                        form.getPrimaryMobile())) {
            bindingResult.rejectValue("primaryMobile", "registration.primaryMobileVerification",
                    "Primary mobile number must be verified through OTP.");
        }

        if (!bindingResult.hasFieldErrors("primaryEmail")
                && !otpVerificationService.isVerified(
                        session,
                        VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT,
                        VerificationChannel.EMAIL,
                        form.getPrimaryEmail())) {
            bindingResult.rejectValue("primaryEmail", "registration.primaryEmailVerification",
                    "Primary email address must be verified.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
