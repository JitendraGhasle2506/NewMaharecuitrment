package com.maharecruitment.gov.in.web.controller.employee;

import java.net.URLConnection;
import java.nio.file.Path;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.recruitment.exception.RecruitmentNotificationException;
import com.maharecruitment.gov.in.common.dto.SessionUserDTO;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileDTO;
import com.maharecruitment.gov.in.web.dto.employee.EmployeeProfileUpdateResponse;
import com.maharecruitment.gov.in.web.service.employee.EmployeeProfileService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/employee")
@PreAuthorize("hasAuthority('ROLE_EMPLOYEE')")
@Slf4j
public class EmployeeDashboardController {

    private static final String SESSION_USER_KEY = "SESSION_USER";
    private static final String EMPLOYEE_PROFILE_REFRESHED_KEY = "employeeProfileSessionRefreshed";

    private final EmployeeProfileService employeeProfileService;

    public EmployeeDashboardController(EmployeeProfileService employeeProfileService) {
        this.employeeProfileService = employeeProfileService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, HttpSession session, Model model) {
        EmployeeProfileDTO profile = employeeProfileService.getCurrentEmployeeProfile(resolveLoginEmail(principal));
        SessionUserDTO loggedInUser = refreshSessionUser(session, profile);
        model.addAttribute("profile", profile);
        model.addAttribute("sessionUser", loggedInUser);
        model.addAttribute("loggedInUser", loggedInUser);
        model.addAttribute("pageHeading", "Employee Dashboard");
        model.addAttribute("pageSubtitle", profile.isProfileAvailable()
                ? "View and update your personal information."
                : "Complete your profile to keep MahaIT employee records up to date.");
        return "employee/dashboard";
    }

    @GetMapping("/profile")
    @ResponseBody
    public ResponseEntity<EmployeeProfileDTO> profile(Principal principal) {
        return ResponseEntity.ok(employeeProfileService.getCurrentEmployeeProfile(resolveLoginEmail(principal)));
    }

    @PostMapping("/profile/update")
    @ResponseBody
    public ResponseEntity<EmployeeProfileUpdateResponse> updateProfile(
            Principal principal,
            HttpSession session,
            @Valid @ModelAttribute EmployeeProfileDTO profileDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            profileDTO.clearEncryptedSubmission();
            return ResponseEntity.badRequest().body(new EmployeeProfileUpdateResponse(
                    false,
                    "Please correct the highlighted fields.",
                    null,
                    fieldErrors(bindingResult)));
        }

        try {
            EmployeeProfileDTO savedProfile = employeeProfileService.updateCurrentEmployeeProfile(
                    resolveLoginEmail(principal),
                    profileDTO);
            refreshSessionUser(session, savedProfile);
            return ResponseEntity.ok(new EmployeeProfileUpdateResponse(
                    true,
                    "Profile updated successfully",
                    savedProfile,
                    Map.of()));
        } catch (RecruitmentNotificationException ex) {
            return ResponseEntity.badRequest().body(new EmployeeProfileUpdateResponse(
                    false,
                    ex.getMessage(),
                    null,
                    Map.of()));
        }
    }

    @PostMapping("/profile/photo/upload")
    @ResponseBody
    public ResponseEntity<EmployeeProfileUpdateResponse> uploadPhoto(
            Principal principal,
            HttpSession session,
            @RequestParam("file") MultipartFile file) {
        try {
            EmployeeProfileDTO savedProfile = employeeProfileService.uploadCurrentEmployeePhoto(
                    resolveLoginEmail(principal),
                    file);
            refreshSessionUser(session, savedProfile);
            return ResponseEntity.ok(new EmployeeProfileUpdateResponse(
                    true,
                    "Photo uploaded successfully.",
                    savedProfile,
                    Map.of()));
        } catch (RecruitmentNotificationException ex) {
            return ResponseEntity.badRequest().body(new EmployeeProfileUpdateResponse(
                    false,
                    ex.getMessage(),
                    null,
                    Map.of()));
        } catch (RuntimeException ex) {
            log.error("Employee profile photo upload failed for login={}", resolveLoginEmail(principal), ex);
            return ResponseEntity.internalServerError().body(new EmployeeProfileUpdateResponse(
                    false,
                    "Unable to upload photo. Please try again after some time.",
                    null,
                    Map.of()));
        }
    }

    @PostMapping("/upload-photo")
    @ResponseBody
    public ResponseEntity<EmployeeProfileUpdateResponse> legacyUploadPhoto(
            Principal principal,
            HttpSession session,
            @RequestParam("file") MultipartFile file) {
        return uploadPhoto(principal, session, file);
    }

    @GetMapping("/profile/photo")
    public ResponseEntity<Resource> photo(Principal principal) {
        try {
            Path path = employeeProfileService.resolveCurrentEmployeePhoto(resolveLoginEmail(principal))
                    .orElse(null);
            if (path == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = URLConnection.guessContentTypeFromName(path.getFileName().toString());
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                    .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String resolveLoginEmail(Principal principal) {
        return principal != null ? principal.getName() : null;
    }

    private Map<String, String> fieldErrors(BindingResult bindingResult) {
        Map<String, String> errors = new LinkedHashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    private SessionUserDTO refreshSessionUser(HttpSession session, EmployeeProfileDTO profile) {
        if (session == null) {
            return null;
        }

        SessionUserDTO sessionUser = extractSessionUser(session);
        if (sessionUser == null) {
            return null;
        }

        SessionUserDTO refreshedUser = new SessionUserDTO(
                sessionUser.id(),
                profile.getFullName(),
                profile.getEmail(),
                sessionUser.roles(),
                sessionUser.departmentId(),
                profile.getMobileNo(),
                profile.getPhotoUrl(),
                sessionUser.loginTime(),
                sessionUser.lastLoginTime());
        session.setAttribute(SESSION_USER_KEY, refreshedUser);
        session.setAttribute(EMPLOYEE_PROFILE_REFRESHED_KEY, Boolean.TRUE);
        return refreshedUser;
    }

    private SessionUserDTO extractSessionUser(HttpSession session) {
        Object candidate = session.getAttribute(SESSION_USER_KEY);
        if (candidate instanceof SessionUserDTO dto) {
            return dto;
        }
        return null;
    }
}
