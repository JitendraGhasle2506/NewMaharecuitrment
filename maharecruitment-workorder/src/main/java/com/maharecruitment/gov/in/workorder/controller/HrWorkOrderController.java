package com.maharecruitment.gov.in.workorder.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.maharecruitment.gov.in.workorder.dto.WorkOrderForm;
import com.maharecruitment.gov.in.workorder.entity.WorkOrderType;
import com.maharecruitment.gov.in.workorder.exception.WorkOrderValidationException;
import com.maharecruitment.gov.in.workorder.service.HrWorkOrderService;
import com.maharecruitment.gov.in.workorder.service.WorkOrderDocumentStorageService;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderDetailView;
import com.maharecruitment.gov.in.workorder.service.model.WorkOrderFormView;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/hr/work-orders")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class HrWorkOrderController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final HrWorkOrderService hrWorkOrderService;
    private final WorkOrderDocumentStorageService workOrderDocumentStorageService;

    public HrWorkOrderController(
            HrWorkOrderService hrWorkOrderService,
            WorkOrderDocumentStorageService workOrderDocumentStorageService) {
        this.hrWorkOrderService = hrWorkOrderService;
        this.workOrderDocumentStorageService = workOrderDocumentStorageService;
    }

    @GetMapping
    public String workOrderList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        model.addAttribute("workOrderPage", hrWorkOrderService.getWorkOrders(PageRequest.of(
                Math.max(page, 0),
                resolvePageSize(size),
                Sort.by(Sort.Order.desc("workOrderDate"), Sort.Order.desc("workOrderId")))));
        return "hr/work-order-list";
    }

    @GetMapping("/new")
    public String createWorkOrderForm(
            @RequestParam(name = "employeeId", required = false) Long employeeId,
            @RequestParam(name = "agencyId", required = false) Long agencyId,
            @RequestParam(name = "recruitmentType", required = false) String recruitmentType,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (agencyId != null || StringUtils.hasText(recruitmentType)) {
            redirectAttributes.addFlashAttribute("workOrderLoadAgencyId", agencyId);
            redirectAttributes.addFlashAttribute("workOrderLoadRecruitmentType", recruitmentType);
            return "redirect:/hr/work-orders/new";
        }

        WorkOrderFormView formView = hrWorkOrderService.prepareCreateForm(
                employeeId,
                resolveLongModelAttribute(model, "workOrderLoadAgencyId"),
                resolveStringModelAttribute(model, "workOrderLoadRecruitmentType"));
        populateFormModel(model, formView, formView.form());
        return "hr/work-order-form";
    }

    @PostMapping("/new")
    public String loadCreateWorkOrderEmployees(
            @RequestParam(name = "agencyId", required = false) Long agencyId,
            @RequestParam(name = "recruitmentType", required = false) String recruitmentType,
            Model model) {
        WorkOrderFormView formView = hrWorkOrderService.prepareCreateForm(null, agencyId, recruitmentType);
        populateFormModel(model, formView, formView.form());
        return "hr/work-order-form";
    }

    @GetMapping("/{workOrderId}/extend")
    public String extensionWorkOrderForm(
            @PathVariable Long workOrderId,
            Model model) {
        WorkOrderFormView formView = hrWorkOrderService.prepareExtensionForm(workOrderId);
        populateFormModel(model, formView, formView.form());
        return "hr/work-order-form";
    }

    @PostMapping
    public String generateWorkOrder(
            @Valid @ModelAttribute("workOrderForm") WorkOrderForm workOrderForm,
            BindingResult bindingResult,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormForValidationFailure(model, workOrderForm);
            return "hr/work-order-form";
        }

        try {
            WorkOrderDetailView detailView = hrWorkOrderService.generateWorkOrder(
                    workOrderForm,
                    principal == null ? null : principal.getName());
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Work order " + detailView.workOrderNumber() + " generated successfully.");
            return "redirect:/hr/work-orders/" + detailView.workOrderId();
        } catch (WorkOrderValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormForValidationFailure(model, workOrderForm);
            return "hr/work-order-form";
        }
    }

    @PostMapping("/preview")
    public String previewWorkOrder(
            @Valid @ModelAttribute("workOrderForm") WorkOrderForm workOrderForm,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            populateFormForValidationFailure(model, workOrderForm);
            return "hr/work-order-form";
        }

        try {
            model.addAttribute("workOrderPreview", hrWorkOrderService.previewWorkOrder(workOrderForm));
            model.addAttribute("workOrderForm", workOrderForm);
            return "hr/work-order-preview";
        } catch (WorkOrderValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormForValidationFailure(model, workOrderForm);
            return "hr/work-order-form";
        }
    }

    @PostMapping("/edit")
    public String editPreviewWorkOrder(
            @ModelAttribute("workOrderForm") WorkOrderForm workOrderForm,
            Model model) {
        populateFormForValidationFailure(model, workOrderForm);
        return "hr/work-order-form";
    }

    @GetMapping("/{workOrderId}")
    public String workOrderDetail(@PathVariable Long workOrderId, Model model) {
        model.addAttribute("workOrderDetail", hrWorkOrderService.getWorkOrderDetail(workOrderId));
        return "hr/work-order-detail";
    }

    @GetMapping("/{workOrderId}/download")
    public ResponseEntity<Resource> downloadWorkOrder(@PathVariable Long workOrderId) {
        String documentPath = hrWorkOrderService.getWorkOrderDocumentPath(workOrderId);
        WorkOrderDetailView detailView = hrWorkOrderService.getWorkOrderDetail(workOrderId);
        Resource resource = workOrderDocumentStorageService.loadAsResource(documentPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(detailView.documentOriginalName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentType(resolveDocumentMediaType(detailView));

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    private MediaType resolveDocumentMediaType(WorkOrderDetailView detailView) {
        if (detailView != null && StringUtils.hasText(detailView.documentContentType())) {
            return MediaType.parseMediaType(detailView.documentContentType());
        }
        return MediaType.APPLICATION_PDF;
    }

    private void populateFormForValidationFailure(Model model, WorkOrderForm workOrderForm) {
        WorkOrderFormView formView = workOrderForm.getWorkOrderType() == WorkOrderType.EXTENSION
                ? hrWorkOrderService.prepareExtensionForm(workOrderForm.getParentWorkOrderId())
                : hrWorkOrderService.prepareCreateForm(
                        null,
                        workOrderForm.getAgencyId(),
                        workOrderForm.getRecruitmentType());
        populateFormModel(model, formView, workOrderForm);
    }

    private void populateFormModel(Model model, WorkOrderFormView formView, WorkOrderForm workOrderForm) {
        model.addAttribute("pageTitle", formView.pageTitle());
        model.addAttribute("pageSubtitle", formView.pageSubtitle());
        model.addAttribute("extensionMode", formView.extensionMode());
        model.addAttribute("parentWorkOrder", formView.parentWorkOrder());
        model.addAttribute("selectedAgencyId", formView.selectedAgencyId());
        model.addAttribute("selectedRecruitmentType", formView.selectedRecruitmentType());
        model.addAttribute("agencyOptions", formView.agencyOptions());
        model.addAttribute("employeeOptions", formView.employeeOptions());
        model.addAttribute("workOrderForm", workOrderForm);
    }

    private int resolvePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Long resolveLongModelAttribute(Model model, String attributeName) {
        Object value = model.asMap().get(attributeName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            try {
                return Long.valueOf(((String) value).trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private String resolveStringModelAttribute(Model model, String attributeName) {
        Object value = model.asMap().get(attributeName);
        return value instanceof String && StringUtils.hasText((String) value) ? ((String) value).trim() : null;
    }
}
