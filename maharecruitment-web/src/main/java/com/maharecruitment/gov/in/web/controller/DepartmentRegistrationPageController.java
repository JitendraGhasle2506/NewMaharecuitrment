package com.maharecruitment.gov.in.web.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
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
import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.service.registration.DepartmentRegistrationPageService;
import com.maharecruitment.gov.in.web.service.verification.OtpVerificationService;
import com.maharecruitment.gov.in.web.service.verification.VerificationPurposes;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/register")
public class DepartmentRegistrationPageController {

    private final DepartmentMstService departmentService;
    private final SubDepartmentService subDepartmentService;
    private final DepartmentRegistrationPageService registrationPageService;
    private final OtpVerificationService otpVerificationService;
    private final NotificationChannelProperties notificationChannelProperties;
    private final boolean otpBypassEnabled;

    public DepartmentRegistrationPageController(
            DepartmentMstService departmentService,
            SubDepartmentService subDepartmentService,
            DepartmentRegistrationPageService registrationPageService,
            OtpVerificationService otpVerificationService,
            NotificationChannelProperties notificationChannelProperties,
            @Value("${registration.department.otp-bypass-enabled:false}") boolean otpBypassEnabled) {
        this.departmentService = departmentService;
        this.subDepartmentService = subDepartmentService;
        this.registrationPageService = registrationPageService;
        this.otpVerificationService = otpVerificationService;
        this.notificationChannelProperties = notificationChannelProperties;
        this.otpBypassEnabled = otpBypassEnabled;
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
            HttpSession session,
            HttpServletRequest request) {
        rejectPlaintextIdentityParameters(request, bindingResult);
        validateDynamicSelections(form, bindingResult, session);
        addGenericSensitiveTransportError(bindingResult);

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
            if (!applyRegistrationError(bindingResult, form, ex)) {
                model.addAttribute("errorMessage", "Unable to complete department registration. Please try again.");
            }
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
        // Ciphertext is single-use request material and must never be reflected into HTML.
        form.clearEncryptedSubmission();
        model.addAttribute("registrationForm", form);
        model.addAttribute("departments", getDepartments());
        model.addAttribute("subDepartments", getSubDepartmentsForForm(form));
        model.addAttribute("primaryMobileVerified",
                !isMobileOtpRequired()
                        || otpVerificationService.isVerified(
                                session,
                                VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT,
                                VerificationChannel.MOBILE,
                                form.getPrimaryMobile()));
        model.addAttribute("primaryEmailVerified",
                !isEmailOtpRequired()
                        || otpVerificationService.isVerified(
                                session,
                                VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT,
                                VerificationChannel.EMAIL,
                                form.getPrimaryEmail()));
        model.addAttribute("otpBypassEnabled", otpBypassEnabled);
        model.addAttribute("mobileOtpEnabled", notificationChannelProperties.isSmsEnabled());
        model.addAttribute("emailOtpEnabled", notificationChannelProperties.isEmailEnabled());
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
        } else {
            if (form.isOtherSubDepartmentSelected() && isBlank(form.getNewSubDeptName())) {
                bindingResult.rejectValue("newSubDeptName", "registration.newSubDeptName",
                        "New sub-department name is required.");
            }
        }

        if (!hasFile(form.getPanFile())) {
            bindingResult.rejectValue("panFile", "registration.panFile", "PAN document is required.");
        }
        if (!hasFile(form.getGstFile())) {
            bindingResult.rejectValue("gstFile", "registration.gstFile", "GST document is required.");
        }
        if (!hasFile(form.getTanFile())) {
            bindingResult.rejectValue("tanFile", "registration.tanFile", "TAN document is required.");
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

        if (isMobileOtpRequired()) {
            if (!bindingResult.hasFieldErrors("primaryMobile")
                    && !otpVerificationService.isVerified(
                            session,
                            VerificationPurposes.DEPARTMENT_REGISTRATION_PRIMARY_CONTACT,
                            VerificationChannel.MOBILE,
                            form.getPrimaryMobile())) {
                bindingResult.rejectValue("primaryMobile", "registration.primaryMobileVerification",
                        "Primary mobile number must be verified through OTP.");
            }
        }

        if (isEmailOtpRequired()) {
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
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isMobileOtpRequired() {
        return !otpBypassEnabled && notificationChannelProperties.isSmsEnabled();
    }

    private boolean isEmailOtpRequired() {
        return !otpBypassEnabled && notificationChannelProperties.isEmailEnabled();
    }

    private boolean applyRegistrationError(
            BindingResult bindingResult,
            DepartmentRegistrationForm form,
            RuntimeException ex) {
        String message = ex.getMessage();
        if (!StringUtils.hasText(message)) {
            return false;
        }

        if ("This department/sub-department combination is already registered.".equals(message)) {
            String field = form.getSubDeptId() != null ? "subDeptId" : "departmentId";
            bindingResult.rejectValue(field, "registration.departmentCombinationDuplicate", message);
            return true;
        }

        if ("A registration already exists for the provided GST number.".equals(message)) {
            bindingResult.reject("registration.gstDuplicate", message);
            return true;
        }

        if ("A registration already exists for the provided PAN number.".equals(message)) {
            bindingResult.reject("registration.panDuplicate", message);
            return true;
        }

        if ("Unable to process the submitted identity information.".equals(message)) {
            bindingResult.reject("registration.sensitiveIdentity", message);
            return true;
        }

        if ("A registration already exists for the provided TAN number.".equals(message)) {
            bindingResult.rejectValue("tanNo", "registration.tanDuplicate", message);
            return true;
        }

        if ("Selected sub-department does not belong to the chosen department.".equals(message)) {
            bindingResult.rejectValue("subDeptId", "registration.subDepartmentMismatch", message);
            return true;
        }

        return false;
    }

    private boolean hasFile(org.springframework.web.multipart.MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void addGenericSensitiveTransportError(BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors("gstNumberEncrypted")
                || bindingResult.hasFieldErrors("panNumberEncrypted")
                || bindingResult.hasFieldErrors("encryptionKeyId")
                || bindingResult.hasFieldErrors("timestamp")
                || bindingResult.hasFieldErrors("nonce")) {
            bindingResult.reject(
                    "registration.sensitiveIdentity",
                    "Unable to process the submitted identity information.");
        }
    }

    private void rejectPlaintextIdentityParameters(HttpServletRequest request, BindingResult bindingResult) {
        if (request.getParameterMap().containsKey("gstNo")
                || request.getParameterMap().containsKey("panNo")
                || request.getParameterMap().containsKey("gstNumber")
                || request.getParameterMap().containsKey("panNumber")
                || request.getParameterMap().containsKey("aadhaarNumber")) {
            bindingResult.reject(
                    "registration.plaintextSensitiveIdentity",
                    "Unable to process the submitted identity information.");
        }
    }
}
