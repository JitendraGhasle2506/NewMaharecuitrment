package com.maharecruitment.gov.in.invoice.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentApplicationStatus;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationActivityEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectResourceRequirementEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentTaxRateMasterEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationActivityRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentTaxRateMasterRepository;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceEntity;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceLineItemEntity;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceNotFoundException;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceNotReadyException;
import com.maharecruitment.gov.in.invoice.repository.DepartmentTaxInvoiceRepository;
import com.maharecruitment.gov.in.invoice.service.DepartmentTaxInvoiceService;
import com.maharecruitment.gov.in.invoice.service.IndianCurrencyToWordsConverter;
import com.maharecruitment.gov.in.invoice.service.TaxInvoiceAmountCalculator;
import com.maharecruitment.gov.in.invoice.service.TaxInvoiceNumberGenerator;
import com.maharecruitment.gov.in.invoice.service.TaxInvoiceViewMapper;
import com.maharecruitment.gov.in.invoice.service.model.TaxInvoiceAmountBreakdown;

@Service
@Transactional(readOnly = true)
public class DepartmentTaxInvoiceServiceImpl implements DepartmentTaxInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentTaxInvoiceServiceImpl.class);

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String DEFAULT_PLACE_OF_SUPPLY = "Maharashtra";
    private static final String DEFAULT_SAC_HSN = "998313";
    private static final String DEFAULT_ACTOR_EMAIL = "SYSTEM";

    private final DepartmentProjectApplicationRepository applicationRepository;
    private final DepartmentRegistrationRepository registrationRepository;
    private final DepartmentTaxRateMasterRepository taxRateMasterRepository;
    private final DepartmentProjectApplicationActivityRepository activityRepository;
    private final MahaItProfileRepository mahaItProfileRepository;
    private final DepartmentTaxInvoiceRepository invoiceRepository;
    private final TaxInvoiceNumberGenerator numberGenerator;
    private final TaxInvoiceAmountCalculator amountCalculator;
    private final IndianCurrencyToWordsConverter currencyToWordsConverter;
    private final TaxInvoiceViewMapper viewMapper;

    public DepartmentTaxInvoiceServiceImpl(
            DepartmentProjectApplicationRepository applicationRepository,
            DepartmentRegistrationRepository registrationRepository,
            DepartmentTaxRateMasterRepository taxRateMasterRepository,
            DepartmentProjectApplicationActivityRepository activityRepository,
            MahaItProfileRepository mahaItProfileRepository,
            DepartmentTaxInvoiceRepository invoiceRepository,
            TaxInvoiceNumberGenerator numberGenerator,
            TaxInvoiceAmountCalculator amountCalculator,
            IndianCurrencyToWordsConverter currencyToWordsConverter,
            TaxInvoiceViewMapper viewMapper) {
        this.applicationRepository = applicationRepository;
        this.registrationRepository = registrationRepository;
        this.taxRateMasterRepository = taxRateMasterRepository;
        this.activityRepository = activityRepository;
        this.mahaItProfileRepository = mahaItProfileRepository;
        this.invoiceRepository = invoiceRepository;
        this.numberGenerator = numberGenerator;
        this.amountCalculator = amountCalculator;
        this.currencyToWordsConverter = currencyToWordsConverter;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional
    public TaxInvoiceView getInvoiceByRequestId(String requestId) {
        String normalizedRequestId = normalizeRequestId(requestId);

        DepartmentTaxInvoiceEntity existingInvoice = invoiceRepository.findByRequestIdIgnoreCase(normalizedRequestId)
                .orElse(null);
        if (existingInvoice != null) {
            return viewMapper.toView(existingInvoice);
        }

        DepartmentProjectApplicationEntity application = applicationRepository.findByRequestIdIgnoreCase(
                normalizedRequestId)
                .orElseThrow(() -> new TaxInvoiceNotFoundException(
                        "Department request not found for request id: " + normalizedRequestId));

        ensureInvoiceIsEligible(application);
        return generateForApplication(application.getDepartmentProjectApplicationId(), resolveActorEmail(application));
    }

    @Override
    @Transactional
    public TaxInvoiceView getInvoiceByApplicationId(Long applicationId) {
        if (applicationId == null) {
            throw new TaxInvoiceException("Application id is required.");
        }

        DepartmentTaxInvoiceEntity existingInvoice = invoiceRepository.findByDepartmentProjectApplicationId(
                applicationId)
                .orElse(null);
        if (existingInvoice != null) {
            return viewMapper.toView(existingInvoice);
        }

        DepartmentProjectApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new TaxInvoiceNotFoundException(
                        "Department request not found for application id: " + applicationId));

        ensureInvoiceIsEligible(application);
        return generateForApplication(applicationId, resolveActorEmail(application));
    }

    @Override
    @Transactional
    public TaxInvoiceView generateForApplication(Long applicationId, String actorEmail) {
        if (applicationId == null) {
            throw new TaxInvoiceException("Application id is required.");
        }

        DepartmentTaxInvoiceEntity existingInvoice = invoiceRepository.findByDepartmentProjectApplicationId(
                applicationId)
                .orElse(null);
        if (existingInvoice != null) {
            return viewMapper.toView(existingInvoice);
        }

        DepartmentProjectApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new TaxInvoiceNotFoundException(
                        "Department request not found for application id: " + applicationId));

        ensureInvoiceIsEligible(application);

        if (application.getDepartmentRegistrationId() == null) {
            throw new TaxInvoiceException("Department registration id is required for tax invoice generation.");
        }

        DepartmentRegistrationEntity registration = registrationRepository.findById(
                application.getDepartmentRegistrationId())
                .orElseThrow(() -> new TaxInvoiceNotFoundException(
                        "Department registration not found for id: " + application.getDepartmentRegistrationId()));

        List<DepartmentProjectResourceRequirementEntity> requirements = application.getResourceRequirements() == null
                ? List.of()
                : application.getResourceRequirements();
        if (requirements.isEmpty()) {
            throw new TaxInvoiceException("Resource requirements are required to generate a tax invoice.");
        }

        LocalDate issueDate = resolveIssueDate(application);
        LocalDate referenceDate = resolveReferenceDate(application, issueDate);
        List<DepartmentTaxInvoiceLineItemEntity> lineItems = buildLineItems(requirements);
        BigDecimal baseAmount = lineItems.stream()
                .map(DepartmentTaxInvoiceLineItemEntity::getTotalAmount)
                .reduce(ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (baseAmount.compareTo(ZERO) <= 0) {
            throw new TaxInvoiceException("Tax invoice base amount must be greater than zero.");
        }

        BigDecimal cgstRate = resolveTaxRate(issueDate, "CGST");
        BigDecimal sgstRate = resolveTaxRate(issueDate, "SGST");
        TaxInvoiceAmountBreakdown breakdown = amountCalculator.calculate(baseAmount, cgstRate, sgstRate);
        MahaItProfile profile = resolveActiveProfile();

        String requestId = requireText(application.getRequestId(), "Request id");

        DepartmentTaxInvoiceEntity invoice = DepartmentTaxInvoiceEntity.builder()
                .departmentProjectApplicationId(application.getDepartmentProjectApplicationId())
                .departmentRegistrationId(application.getDepartmentRegistrationId())
                .requestId(requestId)
                .tiNumber(numberGenerator.generate(issueDate))
                .tiDate(issueDate)
                .deptRefDate(referenceDate)
                .projectName(requireText(application.getProjectName(), "Project name"))
                .projectCode(trimToNull(application.getProjectCode()))
                .pmName(trimToNull(application.getMahaitContact()))
                .billedTo(resolveBilledTo(registration))
                .billingAddress(resolveBillingAddress(registration))
                .clientGstinAvailable(StringUtils.hasText(registration.getGstNo()))
                .clientGstNumber(trimToNull(registration.getGstNo()))
                .placeOfSupply(DEFAULT_PLACE_OF_SUPPLY)
                .baseAmount(breakdown.baseAmount())
                .cgstRate(breakdown.cgstRate())
                .cgstAmount(breakdown.cgstAmount())
                .sgstRate(breakdown.sgstRate())
                .sgstAmount(breakdown.sgstAmount())
                .taxAmount(breakdown.taxAmount())
                .totalAmount(breakdown.totalAmount())
                .companyName(requireText(profile.getCompanyName(), "MahaIT company name"))
                .companyAddress(requireText(profile.getCompanyAddress(), "MahaIT company address"))
                .cinNumber(requireText(profile.getCinNumber(), "MahaIT CIN number"))
                .panNumber(requireText(profile.getPanNumber(), "MahaIT PAN number"))
                .gstNumber(requireText(profile.getGstNumber(), "MahaIT GST number"))
                .bankName(requireText(profile.getBankName(), "MahaIT bank name"))
                .branchName(requireText(profile.getBranchName(), "MahaIT branch name"))
                .accountHolderName(requireText(profile.getAccountHolderName(), "MahaIT account holder name"))
                .accountNumber(requireText(profile.getAccountNumber(), "MahaIT account number"))
                .ifscCode(requireText(profile.getIfscCode(), "MahaIT IFSC code"))
                .amountInWords(currencyToWordsConverter.convert(breakdown.totalAmount()))
                .active(Boolean.TRUE)
                .build();

        invoice.replaceLineItems(lineItems);
        applyAuditMetadata(invoice, actorEmail, application);

        try {
            DepartmentTaxInvoiceEntity saved = invoiceRepository.save(invoice);
            log.info("Tax invoice generated. applicationId={}, requestId={}, tiNumber={}, totalAmount={}",
                    applicationId,
                    application.getRequestId(),
                    saved.getTiNumber(),
                    saved.getTotalAmount());
            return viewMapper.toView(saved);
        } catch (DataIntegrityViolationException ex) {
            DepartmentTaxInvoiceEntity existingAfterConflict = invoiceRepository.findByDepartmentProjectApplicationId(
                    applicationId)
                    .orElseThrow(() -> new TaxInvoiceException(
                            "Unable to persist tax invoice for application id: " + applicationId,
                            ex));
            return viewMapper.toView(existingAfterConflict);
        }
    }

    private void ensureInvoiceIsEligible(DepartmentProjectApplicationEntity application) {
        DepartmentApplicationStatus status = application.getApplicationStatus();
        if (status != DepartmentApplicationStatus.AUDITOR_APPROVED
                && status != DepartmentApplicationStatus.COMPLETED) {
            throw new TaxInvoiceNotReadyException(
                    "Tax invoice is available only after auditor approval.");
        }
    }

    private List<DepartmentTaxInvoiceLineItemEntity> buildLineItems(
            List<DepartmentProjectResourceRequirementEntity> requirements) {
        List<DepartmentTaxInvoiceLineItemEntity> lineItems = new ArrayList<>();

        int lineNumber = 1;
        for (DepartmentProjectResourceRequirementEntity requirement : requirements) {
            if (requirement == null) {
                continue;
            }

            String designationName = requireText(requirement.getDesignationName(),
                    "Resource requirement designation name");
            String levelName = requireText(requirement.getLevelName(), "Resource requirement level name");
            Integer quantity = requirePositive(requirement.getRequiredQuantity(), "Resource requirement quantity");
            Integer durationInMonths = requirePositive(requirement.getDurationInMonths(),
                    "Resource requirement duration");
            BigDecimal ratePerMonth = normalizeCurrency(requirement.getMonthlyRate());
            if (ratePerMonth.compareTo(ZERO) <= 0) {
                throw new TaxInvoiceException("Resource requirement monthly rate must be greater than zero.");
            }

            BigDecimal totalAmount = requirement.getTotalCost() != null
                    ? normalizeCurrency(requirement.getTotalCost())
                    : ratePerMonth
                            .multiply(BigDecimal.valueOf(quantity))
                            .multiply(BigDecimal.valueOf(durationInMonths))
                            .setScale(2, RoundingMode.HALF_UP);

            if (totalAmount.compareTo(ZERO) <= 0) {
                throw new TaxInvoiceException("Resource requirement total cost must be greater than zero.");
            }

            DepartmentTaxInvoiceLineItemEntity lineItem = DepartmentTaxInvoiceLineItemEntity.builder()
                    .departmentProjectResourceRequirementId(requirement.getDepartmentProjectResourceRequirementId())
                    .lineNumber(lineNumber++)
                    .description(designationName + " - " + levelName)
                    .sacHsn(DEFAULT_SAC_HSN)
                    .quantity(quantity)
                    .ratePerMonth(ratePerMonth)
                    .durationInMonths(durationInMonths)
                    .totalAmount(totalAmount)
                    .build();
            lineItems.add(lineItem);
        }

        return lineItems;
    }

    private BigDecimal resolveTaxRate(LocalDate issueDate, String taxCode) {
        return taxRateMasterRepository.findApplicableTaxRates(issueDate)
                .stream()
                .filter(taxRate -> taxCode.equalsIgnoreCase(taxRate.getTaxCode()))
                .map(DepartmentTaxRateMasterEntity::getRatePercentage)
                .filter(rate -> rate != null && rate.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .map(rate -> rate.setScale(4, RoundingMode.HALF_UP))
                .orElse(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
    }

    private MahaItProfile resolveActiveProfile() {
        return mahaItProfileRepository.findFirstByActiveTrueOrderByUpdatedDateDesc()
                .orElseGet(() -> mahaItProfileRepository.findFirstByOrderByUpdatedDateDesc()
                        .orElseThrow(() -> new TaxInvoiceException(
                                "Active MahaIT profile is not configured.")));
    }

    private void applyAuditMetadata(DepartmentTaxInvoiceEntity invoice, String actorEmail,
            DepartmentProjectApplicationEntity application) {
        String auditActor = StringUtils.hasText(actorEmail)
                ? actorEmail.trim()
                : resolveActorEmail(application);
        LocalDateTime now = LocalDateTime.now();
        invoice.setCreatedBy(auditActor);
        invoice.setCreatedDate(now);
        invoice.setUpdatedBy(auditActor);
        invoice.setUpdatedDate(now);
    }

    private LocalDate resolveIssueDate(DepartmentProjectApplicationEntity application) {
        if (application.getDepartmentProjectApplicationId() != null) {
            List<DepartmentProjectApplicationActivityEntity> activities = activityRepository
                    .findByApplicationDepartmentProjectApplicationIdOrderByActionTimestampDesc(
                            application.getDepartmentProjectApplicationId());

            return activities.stream()
                    .filter(activity -> activity != null
                            && activity.getNewStatus() == DepartmentApplicationStatus.AUDITOR_APPROVED
                            && activity.getActionTimestamp() != null)
                    .map(activity -> activity.getActionTimestamp().toLocalDate())
                    .findFirst()
                    .orElseGet(() -> resolveIssueDateFromApplication(application));
        }
        return resolveIssueDateFromApplication(application);
    }

    private LocalDate resolveReferenceDate(DepartmentProjectApplicationEntity application, LocalDate issueDate) {
        if (application.getCreatedDate() != null) {
            return application.getCreatedDate().toLocalDate();
        }
        return issueDate;
    }

    private LocalDate resolveIssueDateFromApplication(DepartmentProjectApplicationEntity application) {
        if (application.getUpdatedDate() != null) {
            return application.getUpdatedDate().toLocalDate();
        }
        if (application.getCreatedDate() != null) {
            return application.getCreatedDate().toLocalDate();
        }
        return LocalDate.now();
    }

    private String resolveBilledTo(DepartmentRegistrationEntity registration) {
        String billedTo = trimToNull(registration.getBillDepartmentName());
        if (billedTo != null) {
            return billedTo;
        }

        billedTo = trimToNull(registration.getDepartmentName());
        if (billedTo != null) {
            return billedTo;
        }

        if (registration.getDepartmentId() != null) {
            return "Department " + registration.getDepartmentId();
        }

        return "Department";
    }

    private String resolveBillingAddress(DepartmentRegistrationEntity registration) {
        String billingAddress = trimToNull(registration.getBillAddress());
        if (billingAddress != null) {
            return billingAddress;
        }

        billingAddress = trimToNull(registration.getAddress());
        if (billingAddress != null) {
            return billingAddress;
        }

        return "Address not available";
    }

    private String resolveActorEmail(DepartmentProjectApplicationEntity application) {
        if (application == null) {
            return DEFAULT_ACTOR_EMAIL;
        }

        if (StringUtils.hasText(application.getUpdatedBy())) {
            return application.getUpdatedBy().trim();
        }
        if (StringUtils.hasText(application.getCreatedBy())) {
            return application.getCreatedBy().trim();
        }
        return DEFAULT_ACTOR_EMAIL;
    }

    private String normalizeRequestId(String requestId) {
        String normalized = trimToNull(requestId);
        if (normalized == null) {
            throw new TaxInvoiceException("Request id is required.");
        }
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new TaxInvoiceException(fieldName + " is required.");
        }
        return normalized;
    }

    private Integer requirePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new TaxInvoiceException(fieldName + " must be greater than zero.");
        }
        return value;
    }

    private BigDecimal normalizeCurrency(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
