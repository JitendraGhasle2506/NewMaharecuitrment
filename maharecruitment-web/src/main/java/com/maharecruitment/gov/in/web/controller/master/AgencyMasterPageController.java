package com.maharecruitment.gov.in.web.controller.master;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.master.dto.AgencyEscalationMatrixResponse;
import com.maharecruitment.gov.in.master.dto.AgencyMasterResponse;
import com.maharecruitment.gov.in.master.entity.AgencyBankAccountType;
import com.maharecruitment.gov.in.master.entity.AgencyEntityType;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.service.AgencyTypeCatalog;
import com.maharecruitment.gov.in.web.dto.master.AgencyEscalationMatrixForm;
import com.maharecruitment.gov.in.web.dto.master.AgencyMasterForm;
import com.maharecruitment.gov.in.web.service.master.AgencyMasterPageService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/master/agencies")
public class AgencyMasterPageController {

    private static final Logger log = LoggerFactory.getLogger(AgencyMasterPageController.class);

    private final AgencyMasterPageService agencyMasterPageService;
    private final AgencyTypeCatalog agencyTypeCatalog;

    public AgencyMasterPageController(
            AgencyMasterPageService agencyMasterPageService,
            AgencyTypeCatalog agencyTypeCatalog) {
        this.agencyMasterPageService = agencyMasterPageService;
        this.agencyTypeCatalog = agencyTypeCatalog;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<AgencyMasterResponse> agencies = agencyMasterPageService.getAll(pageable);
        model.addAttribute("agencies", agencies);
        return "master/agencies/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AgencyMasterForm form = new AgencyMasterForm();
        ensureEscalationRow(form);
        populateForm(model, form, null);
        return "master/agencies/form";
    }

    @GetMapping("/{agencyId}/edit")
    public String editForm(@PathVariable Long agencyId, Model model, RedirectAttributes redirectAttributes) {
        try {
            AgencyMasterResponse existing = agencyMasterPageService.getById(agencyId);
            AgencyMasterForm form = toForm(existing);
            populateForm(model, form, agencyId);
            return "master/agencies/form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/master/agencies";
        }
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("agencyForm") AgencyMasterForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        sanitizeEscalationEntries(form);
        validateEscalationEntries(form, bindingResult);
        validateRequiredFiles(form, bindingResult, false);

        if (bindingResult.hasErrors()) {
            ensureEscalationRow(form);
            populateForm(model, form, null);
            return "master/agencies/form";
        }

        try {
            AgencyMasterResponse response = agencyMasterPageService.create(form);
            buildSuccessMessage(response, redirectAttributes, "Agency created successfully");
            return "redirect:/master/agencies";
        } catch (Exception ex) {
            log.error("Error creating agency", ex);
            model.addAttribute("errorMessage",
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName() + " - check server logs");
            ensureEscalationRow(form);
            populateForm(model, form, null);
            return "master/agencies/form";
        }
    }

    @PostMapping("/{agencyId}")
    public String update(
            @PathVariable Long agencyId,
            @Valid @ModelAttribute("agencyForm") AgencyMasterForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        sanitizeEscalationEntries(form);
        validateEscalationEntries(form, bindingResult);
        validateRequiredFiles(form, bindingResult, true);

        if (bindingResult.hasErrors()) {
            ensureEscalationRow(form);
            populateForm(model, form, agencyId);
            return "master/agencies/form";
        }

        try {
            AgencyMasterResponse response = agencyMasterPageService.update(agencyId, form);
            buildSuccessMessage(response, redirectAttributes, "Agency updated successfully");
            return "redirect:/master/agencies";
        } catch (Exception ex) {
            log.error("Error updating agency id={}", agencyId, ex);
            model.addAttribute("errorMessage",
                    ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName() + " - check server logs");
            ensureEscalationRow(form);
            populateForm(model, form, agencyId);
            return "master/agencies/form";
        }
    }

    @PostMapping("/{agencyId}/status")
    public String updateStatus(
            @PathVariable Long agencyId,
            @RequestParam AgencyStatus status,
            RedirectAttributes redirectAttributes) {
        try {
            agencyMasterPageService.updateStatus(agencyId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Agency status updated successfully");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/master/agencies";
    }

    private void populateForm(Model model, AgencyMasterForm form, Long agencyId) {
        model.addAttribute("agencyForm", form);
        model.addAttribute("agencyId", agencyId);
        model.addAttribute("isEdit", agencyId != null);
        model.addAttribute("agencyTypes", resolveAgencyTypes(form.getAgencyType()));
        model.addAttribute("entityTypes", AgencyEntityType.values());
        model.addAttribute("accountTypes", AgencyBankAccountType.values());
        model.addAttribute("statusOptions", AgencyStatus.values());
    }

    private List<String> resolveAgencyTypes(String selectedAgencyType) {
        List<String> allowedTypes = new ArrayList<>(agencyTypeCatalog.getAllowedTypes());
        if (isBlank(selectedAgencyType)) {
            return allowedTypes;
        }

        String normalizedSelectedType = selectedAgencyType.trim();
        boolean found = allowedTypes.stream()
                .anyMatch(type -> type.equalsIgnoreCase(normalizedSelectedType));
        if (!found) {
            allowedTypes.add(normalizedSelectedType);
        }
        return allowedTypes;
    }

    private AgencyMasterForm toForm(AgencyMasterResponse response) {
        AgencyMasterForm form = new AgencyMasterForm();
        form.setAgencyName(response.getAgencyName());
        form.setOfficialEmail(response.getOfficialEmail());
        form.setTelephoneNumber(response.getTelephoneNumber());
        form.setAgencyType(response.getAgencyType());
        form.setOfficialAddress(response.getOfficialAddress());
        form.setPermanentAddress(response.getPermanentAddress());
        form.setEntityType(response.getEntityType());
        form.setPanNumber(response.getPanNumber());
        form.setExistingPanCopyPath(response.getPanCopyPath());
        form.setCertificateNumber(response.getCertificateNumber());
        form.setExistingCertificateDocumentPath(response.getCertificateDocumentPath());
        form.setGstNumber(response.getGstNumber());
        form.setExistingGstDocumentPath(response.getGstDocumentPath());
        form.setContactPersonName(response.getContactPersonName());
        form.setContactPersonMobileNo(response.getContactPersonMobileNo());
        form.setMsmeRegistered(response.getMsmeRegistered());
        form.setBankName(response.getBankName());
        form.setBankBranch(response.getBankBranch());
        form.setBankAccountNumber(response.getBankAccountNumber());
        form.setBankAccountType(response.getBankAccountType());
        form.setIfscCode(response.getIfscCode());
        form.setExistingCancelledChequePath(response.getCancelledChequePath());
        form.setEscalationMatrixEntries(toEscalationForms(response.getEscalationMatrixEntries()));
        ensureEscalationRow(form);
        return form;
    }

    private List<AgencyEscalationMatrixForm> toEscalationForms(List<AgencyEscalationMatrixResponse> responses) {
        List<AgencyEscalationMatrixForm> forms = new ArrayList<>();
        for (AgencyEscalationMatrixResponse response : responses) {
            AgencyEscalationMatrixForm form = new AgencyEscalationMatrixForm();
            form.setContactName(response.getContactName());
            form.setMobileNumber(response.getMobileNumber());
            form.setLevel(response.getLevel());
            form.setDesignation(response.getDesignation());
            form.setCompanyEmailId(response.getCompanyEmailId());
            forms.add(form);
        }
        return forms;
    }

    private void sanitizeEscalationEntries(AgencyMasterForm form) {
        List<AgencyEscalationMatrixForm> entries = form.getEscalationMatrixEntries();
        if (entries == null) {
            form.setEscalationMatrixEntries(new ArrayList<>());
            return;
        }

        entries.removeIf(this::isEmptyEscalationEntry);
    }

    private boolean isEmptyEscalationEntry(AgencyEscalationMatrixForm entry) {
        return isBlank(entry.getContactName())
                && isBlank(entry.getMobileNumber())
                && isBlank(entry.getLevel())
                && isBlank(entry.getDesignation())
                && isBlank(entry.getCompanyEmailId());
    }

    private void ensureEscalationRow(AgencyMasterForm form) {
        if (form.getEscalationMatrixEntries() == null) {
            form.setEscalationMatrixEntries(new ArrayList<>());
        }
        if (form.getEscalationMatrixEntries().isEmpty()) {
            form.getEscalationMatrixEntries().add(new AgencyEscalationMatrixForm());
        }
    }

    private void validateRequiredFiles(AgencyMasterForm form, BindingResult bindingResult, boolean editMode) {
        if (!editMode || isBlank(form.getExistingPanCopyPath())) {
            if (form.getPanCopyFile() == null || form.getPanCopyFile().isEmpty()) {
                bindingResult.rejectValue("panCopyFile", "agency.panCopyFile", "PAN copy is required.");
            }
        }
        if (!editMode || isBlank(form.getExistingCertificateDocumentPath())) {
            if (form.getCertificateDocumentFile() == null || form.getCertificateDocumentFile().isEmpty()) {
                bindingResult.rejectValue(
                        "certificateDocumentFile",
                        "agency.certificateDocumentFile",
                        "Certificate document is required.");
            }
        }
        if (!editMode || isBlank(form.getExistingGstDocumentPath())) {
            if (form.getGstDocumentFile() == null || form.getGstDocumentFile().isEmpty()) {
                bindingResult.rejectValue("gstDocumentFile", "agency.gstDocumentFile", "GST document is required.");
            }
        }
        if (!editMode || isBlank(form.getExistingCancelledChequePath())) {
            if (form.getCancelledChequeFile() == null || form.getCancelledChequeFile().isEmpty()) {
                bindingResult.rejectValue(
                        "cancelledChequeFile",
                        "agency.cancelledChequeFile",
                        "Cancelled cheque is required.");
            }
        }
    }

    private void validateEscalationEntries(AgencyMasterForm form, BindingResult bindingResult) {
        if (form.getEscalationMatrixEntries() == null || form.getEscalationMatrixEntries().isEmpty()) {
            bindingResult.rejectValue(
                    "escalationMatrixEntries",
                    "agency.escalationMatrixEntries",
                    "At least one escalation matrix entry is required.");
            return;
        }

        for (int index = 0; index < form.getEscalationMatrixEntries().size(); index++) {
            AgencyEscalationMatrixForm entry = form.getEscalationMatrixEntries().get(index);
            validateEscalationEntryField(bindingResult, index, "contactName", entry.getContactName(),
                    "Escalation contact name is required.");
            validateEscalationEntryField(bindingResult, index, "mobileNumber", entry.getMobileNumber(),
                    "Escalation contact mobile number is required.");
            validateEscalationEntryField(bindingResult, index, "level", entry.getLevel(),
                    "Escalation level is required.");
            validateEscalationEntryField(bindingResult, index, "designation", entry.getDesignation(),
                    "Escalation designation is required.");
            validateEscalationEntryField(bindingResult, index, "companyEmailId", entry.getCompanyEmailId(),
                    "Escalation company email id is required.");
        }
    }

    private void validateEscalationEntryField(
            BindingResult bindingResult,
            int index,
            String fieldName,
            String value,
            String message) {
        if (isBlank(value)) {
            bindingResult.addError(new FieldError(
                    "agencyForm",
                    "escalationMatrixEntries[" + index + "]." + fieldName,
                    message));
            return;
        }

        if ("contactName".equals(fieldName) && !value.trim().matches("^[A-Za-z .'-]+$")) {
            bindingResult.addError(new FieldError(
                    "agencyForm",
                    "escalationMatrixEntries[" + index + "]." + fieldName,
                    "Escalation contact name must contain alphabetic characters only."));
        }

        if ("mobileNumber".equals(fieldName) && !value.trim().matches("^[0-9]{10}$")) {
            bindingResult.addError(new FieldError(
                    "agencyForm",
                    "escalationMatrixEntries[" + index + "]." + fieldName,
                    "Escalation contact mobile number must be 10 digits."));
        }

        if ("level".equals(fieldName) && !value.trim().matches("^(L1|L2|L3)$")) {
            bindingResult.addError(new FieldError(
                    "agencyForm",
                    "escalationMatrixEntries[" + index + "]." + fieldName,
                    "Escalation level must be one of L1, L2 or L3."));
        }

        if ("companyEmailId".equals(fieldName) && !value.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            bindingResult.addError(new FieldError(
                    "agencyForm",
                    "escalationMatrixEntries[" + index + "]." + fieldName,
                    "Escalation company email id must be valid."));
        }
    }

    private void buildSuccessMessage(
            AgencyMasterResponse response,
            RedirectAttributes redirectAttributes,
            String defaultMessage) {
        StringBuilder message = new StringBuilder(defaultMessage);
        if (Boolean.TRUE.equals(response.getAgencyUserCreated()) && response.getTemporaryPassword() != null) {
            message.append(". Agency user created for ")
                    .append(response.getProvisionedUserEmail())
                    .append(".");
        }
        redirectAttributes.addFlashAttribute("successMessage", message.toString());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
