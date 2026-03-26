package com.maharecruitment.gov.in.web.controller.common;

import java.security.Principal;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.auth.dto.SessionUserDTO;
import com.maharecruitment.gov.in.auth.dto.UserPasswordChangeRequest;
import com.maharecruitment.gov.in.auth.dto.UserProfileUpdateRequest;
import com.maharecruitment.gov.in.auth.dto.UserProfileView;
import com.maharecruitment.gov.in.auth.service.CurrentUserProfileService;
import com.maharecruitment.gov.in.web.dto.profile.PasswordChangeForm;
import com.maharecruitment.gov.in.web.dto.profile.UserProfileForm;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/common/profile")
public class UserProfilePageController {

    private static final String PROFILE_VIEW = "common/profile";
    private static final String PROFILE_REDIRECT = "redirect:/common/profile";
    private static final String SESSION_USER_KEY = "SESSION_USER";

    private final CurrentUserProfileService currentUserProfileService;

    public UserProfilePageController(CurrentUserProfileService currentUserProfileService) {
        this.currentUserProfileService = currentUserProfileService;
    }

    @GetMapping
    public String profile(Principal principal, HttpSession session, Model model) {
        String actorEmail = resolveActorEmail(principal);
        if (actorEmail == null) {
            return "redirect:/login";
        }

        populatePage(model, actorEmail, session);
        return PROFILE_VIEW;
    }

    @PostMapping
    public String updateProfile(
            Principal principal,
            HttpSession session,
            @Valid @ModelAttribute("profileForm") UserProfileForm profileForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        if (actorEmail == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            populatePage(model, actorEmail, session);
            return PROFILE_VIEW;
        }

        try {
            UserProfileView updatedProfile = currentUserProfileService.updateProfile(actorEmail, toProfileUpdateRequest(profileForm));
            refreshSessionUser(session, updatedProfile);
            redirectAttributes.addFlashAttribute("profileSuccessMessage", "Profile updated successfully.");
            return PROFILE_REDIRECT;
        } catch (RuntimeException ex) {
            model.addAttribute("profileErrorMessage", ex.getMessage());
            populatePage(model, actorEmail, session);
            return PROFILE_VIEW;
        }
    }

    @PostMapping("/password")
    public String changePassword(
            Principal principal,
            HttpSession session,
            @Valid @ModelAttribute("passwordForm") PasswordChangeForm passwordForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        String actorEmail = resolveActorEmail(principal);
        if (actorEmail == null) {
            return "redirect:/login";
        }

        if (!Objects.equals(passwordForm.getNewPassword(), passwordForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.confirm",
                    "New password and confirm password must match.");
        }

        if (bindingResult.hasErrors()) {
            clearPasswordFields(passwordForm);
            populatePage(model, actorEmail, session);
            return PROFILE_VIEW;
        }

        try {
            currentUserProfileService.changePassword(actorEmail, toPasswordChangeRequest(passwordForm));
            redirectAttributes.addFlashAttribute("passwordSuccessMessage", "Password updated successfully.");
            return PROFILE_REDIRECT;
        } catch (RuntimeException ex) {
            clearPasswordFields(passwordForm);
            model.addAttribute("passwordErrorMessage", ex.getMessage());
            populatePage(model, actorEmail, session);
            return PROFILE_VIEW;
        }
    }

    private void populatePage(Model model, String actorEmail, HttpSession session) {
        UserProfileView profileView = currentUserProfileService.getProfile(actorEmail);
        model.addAttribute("profileView", profileView);

        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", toProfileForm(profileView));
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordChangeForm());
        }

        SessionUserDTO sessionUser = extractSessionUser(session);
        model.addAttribute("sessionLoginTime", sessionUser != null ? sessionUser.loginTime() : null);
    }

    private UserProfileForm toProfileForm(UserProfileView profileView) {
        UserProfileForm form = new UserProfileForm();
        form.setName(profileView.getName());
        form.setMobileNo(profileView.getMobileNo());
        return form;
    }

    private UserProfileUpdateRequest toProfileUpdateRequest(UserProfileForm form) {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setName(form.getName());
        request.setMobileNo(form.getMobileNo());
        return request;
    }

    private UserPasswordChangeRequest toPasswordChangeRequest(PasswordChangeForm form) {
        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setCurrentPassword(form.getCurrentPassword());
        request.setNewPassword(form.getNewPassword());
        return request;
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            return null;
        }
        return principal.getName();
    }

    private void refreshSessionUser(HttpSession session, UserProfileView updatedProfile) {
        SessionUserDTO sessionUser = extractSessionUser(session);
        if (sessionUser == null) {
            return;
        }

        session.setAttribute(SESSION_USER_KEY, new SessionUserDTO(
                sessionUser.id(),
                updatedProfile.getName(),
                updatedProfile.getEmail(),
                sessionUser.roles(),
                sessionUser.departmentId(),
                updatedProfile.getMobileNo(),
                sessionUser.loginTime()));
    }

    private SessionUserDTO extractSessionUser(HttpSession session) {
        if (session == null) {
            return null;
        }

        Object candidate = session.getAttribute(SESSION_USER_KEY);
        if (candidate instanceof SessionUserDTO dto) {
            return dto;
        }
        return null;
    }

    private void clearPasswordFields(PasswordChangeForm form) {
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);
    }
}
