package com.maharecruitment.gov.in.department.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.department.dto.DepartmentProfileUpdateForm;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.service.DepartmentProfileDocumentStorageService;
import com.maharecruitment.gov.in.department.service.DepartmentProfileService;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentType;
import com.maharecruitment.gov.in.department.service.model.DepartmentProfileDocumentView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/department/profile")
public class DepartmentProfileController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentProfileController.class);

    private final DepartmentProfileService departmentProfileService;
    private final DepartmentProfileDocumentStorageService documentStorageService;

    public DepartmentProfileController(
            DepartmentProfileService departmentProfileService,
            DepartmentProfileDocumentStorageService documentStorageService) {
        this.departmentProfileService = departmentProfileService;
        this.documentStorageService = documentStorageService;
    }

    @GetMapping
    public String viewProfile(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        String actorEmail = null;
        try {
            actorEmail = resolveActorEmail(principal);
            model.addAttribute("departmentProfile", departmentProfileService.getProfile(actorEmail));
            return "department/profile";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load department profile. actorEmail={}, reason={}", actorEmail, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/department/home";
        }
    }

    @GetMapping("/edit")
    public String editProfile(Model model, Principal principal, RedirectAttributes redirectAttributes) {
        String actorEmail = null;
        try {
            actorEmail = resolveActorEmail(principal);
            model.addAttribute("profileForm", departmentProfileService.getProfileForEdit(actorEmail));
            return "department/profile-edit";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to load profile edit screen. actorEmail={}, reason={}", actorEmail, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/department/profile";
        }
    }

    @PostMapping("/update")
    public String updateProfile(
            @Valid @ModelAttribute("profileForm") DepartmentProfileUpdateForm profileForm,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        String actorEmail = null;
        try {
            actorEmail = resolveActorEmail(principal);

            if (bindingResult.hasErrors()) {
                populateReadOnlyFields(profileForm, actorEmail);
                return "department/profile-edit";
            }

            departmentProfileService.updateProfile(actorEmail, profileForm);
            redirectAttributes.addFlashAttribute("successMessage", "Department profile updated successfully.");
            return "redirect:/department/profile";
        } catch (DepartmentApplicationException ex) {
            log.warn("Unable to update department profile. actorEmail={}, reason={}", actorEmail, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            if (actorEmail != null) {
                populateReadOnlyFields(profileForm, actorEmail);
            }
            return "department/profile-edit";
        }
    }

    @GetMapping("/documents/{documentType}")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable String documentType,
            Principal principal) {
        String actorEmail = resolveActorEmail(principal);
        DepartmentProfileDocumentType resolvedType = DepartmentProfileDocumentType.from(documentType);
        DepartmentProfileDocumentView documentView = departmentProfileService.getProfileDocument(actorEmail, resolvedType);
        Resource resource = documentStorageService.loadAsResource(documentView.getFullPath());

        String contentType = documentView.getContentType() != null
                ? documentView.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(documentView.getOriginalFileName())
                        .build()
                        .toString())
                .body(resource);
    }

    private String resolveActorEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new DepartmentApplicationException("Authenticated user is required.");
        }
        return principal.getName().trim();
    }

    private void populateReadOnlyFields(DepartmentProfileUpdateForm profileForm, String actorEmail) {
        DepartmentProfileUpdateForm loadedForm = departmentProfileService.getProfileForEdit(actorEmail);
        profileForm.setDepartmentName(loadedForm.getDepartmentName());
        profileForm.setSubDepartmentName(loadedForm.getSubDepartmentName());
        profileForm.setExistingGstDocumentName(loadedForm.getExistingGstDocumentName());
        profileForm.setExistingPanDocumentName(loadedForm.getExistingPanDocumentName());
        profileForm.setExistingTanDocumentName(loadedForm.getExistingTanDocumentName());
    }
}
