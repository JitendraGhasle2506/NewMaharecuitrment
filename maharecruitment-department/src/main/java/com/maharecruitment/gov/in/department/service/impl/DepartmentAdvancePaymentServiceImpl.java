package com.maharecruitment.gov.in.department.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.UserRepository;
import com.maharecruitment.gov.in.department.dto.AdvancePaymentForm;
import com.maharecruitment.gov.in.department.entity.DepartmentProformaInvoiceEntity;
import com.maharecruitment.gov.in.department.dto.DepartmentProjectApplicationSummaryView;
import com.maharecruitment.gov.in.department.entity.AuditorReviewDecision;
import com.maharecruitment.gov.in.department.entity.DepartmentAdvancePaymentActivityEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentAdvancePaymentEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationActivityType;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.HrReviewDecision;
import com.maharecruitment.gov.in.department.exception.DepartmentApplicationException;
import com.maharecruitment.gov.in.department.repository.DepartmentAdvancePaymentActivityRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentAdvancePaymentRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.service.DepartmentAdvancePaymentService;
import com.maharecruitment.gov.in.department.service.DepartmentPaymentStorageService;
import com.maharecruitment.gov.in.department.service.model.DepartmentActorContext;
import com.maharecruitment.gov.in.department.service.model.StoredDocument;
import com.maharecruitment.gov.in.master.dto.ProformaInvoiceSummary;

@Service
@Transactional(readOnly = true)
public class DepartmentAdvancePaymentServiceImpl implements DepartmentAdvancePaymentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentAdvancePaymentServiceImpl.class);

    private static final String ACTION_SAVE = "SAVE";
    private static final String ACTION_SEND = "SEND";

    private final DepartmentAdvancePaymentRepository paymentRepository;
    private final DepartmentAdvancePaymentActivityRepository activityRepository;
    private final DepartmentProjectApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final DepartmentPaymentStorageService storageService;
    private final com.maharecruitment.gov.in.department.repository.DepartmentProformaInvoiceRepository proformaInvoiceRepository;

    public DepartmentAdvancePaymentServiceImpl(
            DepartmentAdvancePaymentRepository paymentRepository,
            DepartmentAdvancePaymentActivityRepository activityRepository,
            DepartmentProjectApplicationRepository applicationRepository,
            UserRepository userRepository,
            DepartmentPaymentStorageService storageService,
            com.maharecruitment.gov.in.department.repository.DepartmentProformaInvoiceRepository proformaInvoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.activityRepository = activityRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.proformaInvoiceRepository = proformaInvoiceRepository;
    }

    @Override
    public AdvancePaymentForm initializePaymentForm(Long applicationId, String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        DepartmentProjectApplicationEntity app = findOwnedApplication(applicationId, actorContext);

        // Check if payment already exists for this application
        if (paymentRepository.existsByApplication(app)) {
            throw new DepartmentApplicationException("An advance payment has already been initiated for this project.");
        }

        AdvancePaymentForm form = new AdvancePaymentForm();
        form.setDepartmentProjectApplicationId(app.getDepartmentProjectApplicationId());
        form.setDepartmentRegistrationId(actorContext.getDepartmentRegistrationId());

        // Fetch latest PI details
        List<DepartmentProformaInvoiceEntity> invoices = proformaInvoiceRepository
                .findByApplication_DepartmentProjectApplicationIdOrderByDepartmentProformaInvoiceIdDesc(applicationId);
        
        if (!invoices.isEmpty()) {
            DepartmentProformaInvoiceEntity pi = invoices.get(0);
            form.setProformaInvoiceId(pi.getPiNumber());
            form.setPiNumber(pi.getPiNumber());
            form.setTotalPiAmount(pi.getTotalAmount());
            
            // Calculate previously paid amounts for this application (excluding current if editing, but this is initialize)
            List<DepartmentAdvancePaymentEntity> pastPayments = paymentRepository
                    .findByApplicationAndApplicationStatusNotIn(app, List.of(DepartmentApplicationStatus.HR_REJECTED));
            
            BigDecimal paidAmount = pastPayments.stream()
                    .map(DepartmentAdvancePaymentEntity::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            form.setPartialAmount(paidAmount);
            form.setBalanceAmount(pi.getTotalAmount().subtract(paidAmount));
            form.setTotalAmount(pi.getTotalAmount().subtract(paidAmount)); // Default to paying balance
            form.setPaymentType("FULL");
        }

        return form;
    }

    @Override
    public DepartmentProjectApplicationSummaryView getProjectApplicationSummary(Long applicationId) {
        DepartmentProjectApplicationEntity entity = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new DepartmentApplicationException("Application not found."));

        return DepartmentProjectApplicationSummaryView.builder()
                .departmentProjectApplicationId(entity.getDepartmentProjectApplicationId())
                .requestId(entity.getRequestId())
                .projectName(entity.getProjectName())
                .projectCode(entity.getProjectCode())
                .applicationStatus(entity.getApplicationStatus())
                .totalEstimatedCost(entity.getTotalEstimatedCost())
                .build();
    }

    @Override
    public List< ProformaInvoiceSummary> getAvailableInvoices(Long applicationId) {
        return proformaInvoiceRepository.findByApplication_DepartmentProjectApplicationIdOrderByDepartmentProformaInvoiceIdDesc(applicationId)
                .stream()
                .map(pi -> new ProformaInvoiceSummary(pi.getPiNumber(), pi.getTotalAmount()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long savePayment(AdvancePaymentForm form, String actionType, String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        String normalizedAction = actionType != null ? actionType.trim().toUpperCase(Locale.ROOT) : ACTION_SAVE;

        DepartmentAdvancePaymentEntity entity;
        boolean isNew = form.getId() == null;

        if (isNew) {
            DepartmentProjectApplicationEntity app = findOwnedApplication(form.getDepartmentProjectApplicationId(),
                    actorContext);

            // Guard against duplicate creation
            if (paymentRepository.existsByApplication(app)) {
                throw new DepartmentApplicationException(
                        "An advance payment record already exists for this project application.");
            }

            entity = new DepartmentAdvancePaymentEntity();
            entity.setApplication(app);
            entity.setDepartmentRegistrationId(actorContext.getDepartmentRegistrationId());
            entity.setCreatedBy(actorEmail);
            entity.setCreatedDate(LocalDateTime.now());
        } else {
            entity = paymentRepository.findById(form.getId())
                    .orElseThrow(() -> new DepartmentApplicationException("Payment record not found."));

            // Check authorization
            if (!entity.getDepartmentRegistrationId().equals(actorContext.getDepartmentRegistrationId())) {
                throw new DepartmentApplicationException("Unauthorized access to payment record.");
            }

            // Guard against modifying submitted records
            if (entity.getApplicationStatus() != DepartmentApplicationStatus.DRAFT &&
                    entity.getApplicationStatus() != DepartmentApplicationStatus.HR_SENT_BACK &&
                    entity.getApplicationStatus() != DepartmentApplicationStatus.AUDITOR_SENT_BACK) {
                throw new DepartmentApplicationException(
                        "This payment record has been submitted and cannot be modified in its current status: "
                                + entity.getApplicationStatus());
            }
        }

        entity.setProformaInvoiceId(form.getProformaInvoiceId());
        entity.setReceiptNumber(form.getUtrNumber()); // Consolidate: Receipt Number = UTR Number
        entity.setTotalAmount(form.getTotalAmount());
        entity.setRemarks(form.getRemarks());
        entity.setUtrNumber(form.getUtrNumber());
        entity.setUpdatedBy(actorEmail);
        entity.setUpdatedDate(LocalDateTime.now());

        handleReceiptUpload(form, entity);

        if (ACTION_SEND.equals(normalizedAction)) {
            validateForSubmission(entity);
            entity.setApplicationStatus(DepartmentApplicationStatus.SUBMITTED_TO_HR);
        } else {
            entity.setApplicationStatus(DepartmentApplicationStatus.DRAFT);
        }

        DepartmentAdvancePaymentEntity saved = paymentRepository.save(entity);
        log.info("Advance payment record saved. id={}, status={}, actor={}", saved.getId(),
                saved.getApplicationStatus(), actorEmail);

        logActivity(saved, isNew ? DepartmentApplicationActivityType.CREATED : DepartmentApplicationActivityType.UPDATED,
                null, saved.getApplicationStatus(), actorContext, form.getRemarks());

        return saved.getId();
    }

    @Override
    public AdvancePaymentForm getPaymentForEdit(Long paymentId, String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        DepartmentAdvancePaymentEntity entity = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DepartmentApplicationException("Payment record not found."));

        // Check authorization
        if (!entity.getDepartmentRegistrationId().equals(actorContext.getDepartmentRegistrationId())) {
            throw new DepartmentApplicationException("Unauthorized access to payment record.");
        }

        // Guard against editing submitted records
        if (entity.getApplicationStatus() != DepartmentApplicationStatus.DRAFT &&
                entity.getApplicationStatus() != DepartmentApplicationStatus.HR_SENT_BACK &&
                entity.getApplicationStatus() != DepartmentApplicationStatus.AUDITOR_SENT_BACK) {
            throw new DepartmentApplicationException(
                    "This payment record has been submitted and cannot be modified. Status: "
                            + entity.getApplicationStatus());
        }

        AdvancePaymentForm form = new AdvancePaymentForm();
        form.setId(entity.getId());
        form.setApplicationStatus(entity.getApplicationStatus());
        form.setDepartmentProjectApplicationId(entity.getApplication().getDepartmentProjectApplicationId());
        form.setDepartmentRegistrationId(entity.getDepartmentRegistrationId());
        form.setProformaInvoiceId(entity.getProformaInvoiceId());
        form.setReceiptNumber(entity.getReceiptNumber());
        form.setTotalAmount(entity.getTotalAmount());
        form.setRemarks(entity.getRemarks());
        form.setUtrNumber(entity.getUtrNumber());
        form.setReceiptOriginalName(entity.getReceiptOriginalName());
        form.setReceiptFileType(entity.getReceiptFileType());
        
        populatePiInfoInForm(form, entity.getApplication());
        
        return form;
    }

    @Override
    public List<DepartmentAdvancePaymentEntity> getPaymentSummaries(String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        return paymentRepository
                .findByDepartmentRegistrationIdOrderByIdDesc(actorContext.getDepartmentRegistrationId());
    }

    @Override
    public List<DepartmentProjectApplicationSummaryView> getEligibleProjectsForAdvancePayment(String actorEmail) {
        DepartmentActorContext actorContext = resolveDepartmentActorContext(actorEmail);
        List<DepartmentProjectApplicationEntity> applications = applicationRepository
                .findByDepartmentRegistrationIdAndApplicationStatusInOrderByDepartmentProjectApplicationIdDesc(
                        actorContext.getDepartmentRegistrationId(),
                        List.of(DepartmentApplicationStatus.COMPLETED));

        // Use explicit ID-based check for robustness
        List<Long> initiatedAppIds = paymentRepository
                .findApplicationIdsByDepartmentRegistrationId(actorContext.getDepartmentRegistrationId());

        return applications.stream()
                .filter(entity -> !initiatedAppIds.contains(entity.getDepartmentProjectApplicationId()))
                .map(entity -> DepartmentProjectApplicationSummaryView.builder()
                        .departmentProjectApplicationId(entity.getDepartmentProjectApplicationId())
                        .requestId(entity.getRequestId())
                        .projectName(entity.getProjectName())
                        .projectCode(entity.getProjectCode())
                        .applicationStatus(entity.getApplicationStatus())
                        .totalEstimatedCost(entity.getTotalEstimatedCost())
                        .build())
                .toList();
    }

    @Override
    public boolean isReceiptNumberDuplicate(String receiptNumber, Long paymentId) {
        if (!StringUtils.hasText(receiptNumber))
            return false;
        if (paymentId == null) {
            return paymentRepository.existsByReceiptNumber(receiptNumber.trim());
        }
        return paymentRepository.existsByReceiptNumberAndIdNot(receiptNumber.trim(), paymentId);
    }

    @Override
    @Transactional
    public void reviewByHr(Long paymentId, HrReviewDecision decision, String remarks, String actorEmail) {
        DepartmentAdvancePaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DepartmentApplicationException("Payment record not found."));

        if (payment.getApplicationStatus() != DepartmentApplicationStatus.SUBMITTED_TO_HR) {
            throw new DepartmentApplicationException("Payment is not in a state that can be reviewed by HR.");
        }

        DepartmentApplicationStatus oldStatus = payment.getApplicationStatus();

        switch (decision) {
            case APPROVE -> payment.setApplicationStatus(DepartmentApplicationStatus.AUDITOR_REVIEW);
            case REJECT -> payment.setApplicationStatus(DepartmentApplicationStatus.HR_REJECTED);
            case SEND_BACK -> payment.setApplicationStatus(DepartmentApplicationStatus.HR_SENT_BACK);
        }

        payment.setRemarks(remarks);
        payment.setUpdatedBy(actorEmail);
        payment.setUpdatedDate(LocalDateTime.now());
        DepartmentAdvancePaymentEntity saved = paymentRepository.save(payment);

        DepartmentActorContext actorContext = resolveActorContext(actorEmail);
        logActivity(saved, DepartmentApplicationActivityType.HR_REVIEWED,
                oldStatus, saved.getApplicationStatus(), actorContext, remarks);
    }

    @Override
    @Transactional
    public void reviewByAuditor(Long paymentId, AuditorReviewDecision decision, String remarks, String actorEmail) {
        DepartmentAdvancePaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DepartmentApplicationException("Payment record not found."));

        if (payment.getApplicationStatus() != DepartmentApplicationStatus.AUDITOR_REVIEW) {
            throw new DepartmentApplicationException("Payment is not in a state that can be reviewed by Auditor.");
        }

        DepartmentApplicationStatus oldStatus = payment.getApplicationStatus();

        switch (decision) {
            case APPROVE -> payment.setApplicationStatus(DepartmentApplicationStatus.AUDITOR_APPROVED);
            case SEND_BACK -> payment.setApplicationStatus(DepartmentApplicationStatus.AUDITOR_SENT_BACK);
        }

        payment.setRemarks(remarks);
        payment.setUpdatedBy(actorEmail);
        payment.setUpdatedDate(LocalDateTime.now());
        DepartmentAdvancePaymentEntity saved = paymentRepository.save(payment);

        DepartmentActorContext actorContext = resolveActorContext(actorEmail);
        logActivity(saved, DepartmentApplicationActivityType.AUDITOR_REVIEWED,
                oldStatus, saved.getApplicationStatus(), actorContext, remarks);
    }

    @Override
    public List<DepartmentAdvancePaymentEntity> getReviewList(String actorEmail) {
        User user = userRepository.findByEmailIgnoreCase(actorEmail).orElse(null);
        if (user == null) {
            return List.of();
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        List<DepartmentApplicationStatus> statuses = new ArrayList<>();
        if (roles.contains("ROLE_HR") || roles.contains("HR")) {
            statuses.add(DepartmentApplicationStatus.SUBMITTED_TO_HR);
        }
        if (roles.contains("ROLE_AUDITOR") || roles.contains("AUDITOR")) {
            statuses.add(DepartmentApplicationStatus.AUDITOR_REVIEW);
        }

        if (statuses.isEmpty()) {
            return List.of();
        }

        return paymentRepository.findByApplicationStatusInOrderByIdDesc(statuses);
    }

    @Override
    public AdvancePaymentForm getPaymentForReview(Long paymentId, String actorEmail) {
        User user = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new DepartmentApplicationException("User not found."));

        DepartmentAdvancePaymentEntity entity = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DepartmentApplicationException("Payment record not found."));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isAuthorized = false;
        if (roles.contains("ROLE_HR") || roles.contains("HR")) {
            isAuthorized = true;
        } else if (roles.contains("ROLE_AUDITOR") || roles.contains("AUDITOR")) {
            isAuthorized = true;
        } else if (user.getDepartmentRegistrationId() != null
                && entity.getDepartmentRegistrationId()
                        .equals(user.getDepartmentRegistrationId().getDepartmentRegistrationId())) {
            isAuthorized = true;
        }

        if (!isAuthorized) {
            throw new DepartmentApplicationException("You are not authorized to view this payment record.");
        }

        AdvancePaymentForm form = new AdvancePaymentForm();
        form.setId(entity.getId());
        form.setApplicationStatus(entity.getApplicationStatus());
        form.setDepartmentProjectApplicationId(entity.getApplication().getDepartmentProjectApplicationId());
        form.setDepartmentRegistrationId(entity.getDepartmentRegistrationId());
        form.setProformaInvoiceId(entity.getProformaInvoiceId());
        form.setReceiptNumber(entity.getReceiptNumber());
        form.setTotalAmount(entity.getTotalAmount());
        form.setRemarks(entity.getRemarks());
        form.setUtrNumber(entity.getUtrNumber());
        form.setReceiptOriginalName(entity.getReceiptOriginalName());
        form.setReceiptFileType(entity.getReceiptFileType());
        
        populatePiInfoInForm(form, entity.getApplication());
        
        return form;
    }

    @Override
    public Resource getReceiptResource(Long paymentId, String actorEmail) {
        DepartmentAdvancePaymentEntity entity = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new DepartmentApplicationException("Payment record not found."));

        User user = userRepository.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new DepartmentApplicationException("User not found."));

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        boolean isAuthorized = false;
        if (roles.contains("ROLE_HR") || roles.contains("HR")) {
            isAuthorized = true;
        } else if (roles.contains("ROLE_AUDITOR") || roles.contains("AUDITOR")) {
            isAuthorized = true;
        } else if (user.getDepartmentRegistrationId() != null
                && entity.getDepartmentRegistrationId()
                        .equals(user.getDepartmentRegistrationId().getDepartmentRegistrationId())) {
            isAuthorized = true;
        }

        if (!isAuthorized) {
            throw new DepartmentApplicationException("You are not authorized to view this document.");
        }

        return storageService.loadAsResource(entity.getReceiptFilePath());
    }

    @Override
    public List<DepartmentAdvancePaymentActivityEntity> getActivitiesByPaymentId(Long paymentId) {
        return activityRepository.findByPaymentIdOrderByActionTimestampDesc(paymentId);
    }

    private void logActivity(DepartmentAdvancePaymentEntity payment,
            DepartmentApplicationActivityType type,
            DepartmentApplicationStatus oldStatus,
            DepartmentApplicationStatus newStatus,
            DepartmentActorContext context,
            String remarks) {
        DepartmentAdvancePaymentActivityEntity activity = new DepartmentAdvancePaymentActivityEntity();
        activity.setPayment(payment);
        activity.setActivityType(type);
        activity.setPreviousStatus(oldStatus);
        activity.setNewStatus(newStatus);
        activity.setActorUserId(context.getUserId());
        activity.setActorEmail(context.getActorEmail());
        activity.setActorName(context.getActorName());
        activity.setActivityRemarks(remarks);
        activity.setActionTimestamp(LocalDateTime.now());
        activityRepository.save(activity);
    }

    private void handleReceiptUpload(AdvancePaymentForm form, DepartmentAdvancePaymentEntity entity) {
        if (form.getReceiptFile() != null && !form.getReceiptFile().isEmpty()) {
            StoredDocument stored = storageService.storePaymentReceipt(form.getReceiptFile(),
                    entity.getReceiptFilePath());
            entity.setReceiptOriginalName(stored.getOriginalFileName());
            entity.setReceiptFilePath(stored.getFullPath());
            entity.setReceiptFileType(stored.getContentType());
            entity.setReceiptFileSize(stored.getFileSize());
        }
    }

    private void validateForSubmission(DepartmentAdvancePaymentEntity entity) {
        if (!StringUtils.hasText(entity.getProformaInvoiceId())) {
            throw new DepartmentApplicationException("Proforma Invoice is required for submission.");
        }
        if (!StringUtils.hasText(entity.getReceiptNumber())) {
            throw new DepartmentApplicationException("Receipt number is required for submission.");
        }
        if (entity.getTotalAmount() == null || entity.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DepartmentApplicationException("Valid total amount is required for submission.");
        }
        if (!StringUtils.hasText(entity.getReceiptFilePath())) {
            throw new DepartmentApplicationException("Proof of payment document is required for submission.");
        }
    }

    private DepartmentProjectApplicationEntity findOwnedApplication(Long applicationId,
            DepartmentActorContext actorContext) {
        return applicationRepository
                .findByDepartmentProjectApplicationIdAndDepartmentRegistrationId(applicationId,
                        actorContext.getDepartmentRegistrationId())
                .orElseThrow(() -> new DepartmentApplicationException("Project application not found."));
    }

    private DepartmentActorContext resolveActorContext(String actorEmail) {
        User user = userRepository.findByEmailIgnoreCase(actorEmail).orElse(null);
        if (user == null) {
            throw new DepartmentApplicationException("User not found.");
        }

        var builder = DepartmentActorContext.builder()
                .userId(user.getId())
                .actorName(user.getName())
                .actorEmail(user.getEmail());

        if (user.getDepartmentRegistrationId() != null) {
            DepartmentRegistrationEntity reg = user.getDepartmentRegistrationId();
            builder.departmentId(reg.getDepartmentId())
                    .departmentRegistrationId(reg.getDepartmentRegistrationId());
        }

        return builder.build();
    }

    private DepartmentActorContext resolveDepartmentActorContext(String actorEmail) {
        DepartmentActorContext context = resolveActorContext(actorEmail);
        if (context.getDepartmentRegistrationId() == null) {
            throw new DepartmentApplicationException("Department profile not found for user.");
        }
        return context;
    }

    private void populatePiInfoInForm(AdvancePaymentForm form, DepartmentProjectApplicationEntity app) {
        List<DepartmentProformaInvoiceEntity> invoices = proformaInvoiceRepository
                .findByApplication_DepartmentProjectApplicationIdOrderByDepartmentProformaInvoiceIdDesc(
                        app.getDepartmentProjectApplicationId());
        
        if (!invoices.isEmpty()) {
            DepartmentProformaInvoiceEntity pi = invoices.get(0);
            form.setPiNumber(pi.getPiNumber());
            form.setTotalPiAmount(pi.getTotalAmount());
            
            List<DepartmentAdvancePaymentEntity> pastPayments = paymentRepository
                    .findByApplicationAndApplicationStatusNotIn(app, List.of(DepartmentApplicationStatus.HR_REJECTED));
            
            BigDecimal paidAmount = pastPayments.stream()
                    .filter(p -> form.getId() == null || !p.getId().equals(form.getId()))
                    .map(DepartmentAdvancePaymentEntity::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            form.setPartialAmount(paidAmount);
            form.setBalanceAmount(pi.getTotalAmount().subtract(paidAmount));
        }
    }
}
