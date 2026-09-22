package com.maharecruitment.gov.in.invoice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import com.maharecruitment.gov.in.auth.repository.DepartmentRegistrationRepository;
import com.maharecruitment.gov.in.common.mahaitprofile.entity.MahaItProfile;
import com.maharecruitment.gov.in.common.mahaitprofile.repository.MahaItProfileRepository;
import com.maharecruitment.gov.in.department.entity.DepartmentProjectApplicationEntity;
import com.maharecruitment.gov.in.department.entity.DepartmentTaxRateMasterEntity;
import com.maharecruitment.gov.in.department.repository.DepartmentProjectApplicationRepository;
import com.maharecruitment.gov.in.department.repository.DepartmentTaxRateMasterRepository;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceLineItemView;
import com.maharecruitment.gov.in.invoice.dto.TaxInvoiceView;
import com.maharecruitment.gov.in.invoice.entity.DepartmentTaxInvoiceEntity;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.service.model.TaxInvoiceAmountBreakdown;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationRate;
import com.maharecruitment.gov.in.master.entity.ProjectMst;
import com.maharecruitment.gov.in.master.entity.RateMaster;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationRateRepository;
import com.maharecruitment.gov.in.master.repository.RateMasterRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeProjectMappingEntity;

/**
 * Builds a proforma tax invoice for a project where every line item is one mapped employee,
 * priced from that employee's designation/level rate and prorated to the days worked in the period.
 */
@Component
public class EmployeeTaxInvoiceBuilder {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String DEFAULT_PLACE_OF_SUPPLY = "Maharashtra";
    private static final String DEFAULT_SAC_HSN = "998313";
    private static final DateTimeFormatter PERIOD_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter REFERENCE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DepartmentRegistrationRepository registrationRepository;
    private final DepartmentProjectApplicationRepository applicationRepository;
    private final DepartmentTaxRateMasterRepository taxRateMasterRepository;
    private final MahaItProfileRepository mahaItProfileRepository;
    private final ManpowerDesignationRateRepository designationRateRepository;
    private final RateMasterRepository rateMasterRepository;
    private final TaxInvoiceAmountCalculator amountCalculator;
    private final IndianCurrencyToWordsConverter currencyToWordsConverter;
    private final TaxInvoiceDisplayFormatter displayFormatter;
    private final TaxInvoiceViewMapper viewMapper;

    public EmployeeTaxInvoiceBuilder(
            DepartmentRegistrationRepository registrationRepository,
            DepartmentProjectApplicationRepository applicationRepository,
            DepartmentTaxRateMasterRepository taxRateMasterRepository,
            MahaItProfileRepository mahaItProfileRepository,
            ManpowerDesignationRateRepository designationRateRepository,
            RateMasterRepository rateMasterRepository,
            TaxInvoiceAmountCalculator amountCalculator,
            IndianCurrencyToWordsConverter currencyToWordsConverter,
            TaxInvoiceDisplayFormatter displayFormatter,
            TaxInvoiceViewMapper viewMapper) {
        this.registrationRepository = registrationRepository;
        this.applicationRepository = applicationRepository;
        this.taxRateMasterRepository = taxRateMasterRepository;
        this.mahaItProfileRepository = mahaItProfileRepository;
        this.designationRateRepository = designationRateRepository;
        this.rateMasterRepository = rateMasterRepository;
        this.amountCalculator = amountCalculator;
        this.currencyToWordsConverter = currencyToWordsConverter;
        this.displayFormatter = displayFormatter;
        this.viewMapper = viewMapper;
    }

    public TaxInvoiceView build(
            ProjectMst project,
            Long departmentId,
            Long subDepartmentId,
            List<EmployeeProjectMappingEntity> mappings,
            LocalDate periodStart,
            LocalDate periodEnd) {
        if (project == null) {
            throw new TaxInvoiceException("Select a project to generate the tax invoice.");
        }
        mappings = InvoiceEmployeeMappings.uniqueEmployees(mappings);
        if (mappings.isEmpty()) {
            throw new TaxInvoiceException("No active employees are mapped to the selected project.");
        }

        LocalDate issueDate = LocalDate.now();
        BigDecimal agencyCommissionMultiplier = resolveCommissionMultiplier("AGENCY");
        BigDecimal mahaItCommissionMultiplier = resolveCommissionMultiplier("MAHAIT");

        List<EmployeeLine> lines = new ArrayList<>();
        for (EmployeeProjectMappingEntity mapping : mappings) {
            EmployeeLine line = buildEmployeeLine(
                    mapping.getEmployee(),
                    periodStart,
                    periodEnd,
                    agencyCommissionMultiplier,
                    mahaItCommissionMultiplier);
            if (line != null) {
                lines.add(line);
            }
        }
        if (lines.isEmpty()) {
            throw new TaxInvoiceException(
                    "None of the mapped employees were working on the project between "
                            + PERIOD_DATE_FORMATTER.format(periodStart) + " and "
                            + PERIOD_DATE_FORMATTER.format(periodEnd) + ".");
        }

        BigDecimal totalAgencyCommission = sum(lines.stream().map(EmployeeLine::agencyCommissionAmount).toList());
        BigDecimal totalMahaItCommission = sum(lines.stream().map(EmployeeLine::mahaItCommissionAmount).toList());
        BigDecimal taxableBase = sum(lines.stream().map(EmployeeLine::taxableAmount).toList());
        if (taxableBase.compareTo(ZERO) <= 0) {
            throw new TaxInvoiceException("Tax invoice base amount must be greater than zero.");
        }

        TaxInvoiceAmountBreakdown breakdown = amountCalculator.calculate(
                taxableBase,
                resolveTaxRate(issueDate, "CGST"),
                resolveTaxRate(issueDate, "SGST"));
        DepartmentProjectApplicationEntity application = resolveProjectApplication(project);
        DepartmentRegistrationEntity registration = resolveRegistration(
                project, departmentId, subDepartmentId, application, mappings);
        MahaItProfile profile = resolveActiveProfile();

        // Built only to reuse the shared view mapping (masking, amount formatting, QR); never persisted.
        DepartmentTaxInvoiceEntity invoice = DepartmentTaxInvoiceEntity.builder()
                .departmentProjectApplicationId(application == null ? null
                        : application.getDepartmentProjectApplicationId())
                .departmentRegistrationId(registration == null ? null : registration.getDepartmentRegistrationId())
                .requestId(application == null ? null : trimToNull(application.getRequestId()))
                .tiNumber(buildReferenceNumber(project, periodStart, periodEnd))
                .tiDate(issueDate)
                .deptRefDate(application != null && application.getCreatedDate() != null
                        ? application.getCreatedDate().toLocalDate()
                        : periodStart)
                .projectName(requireText(project.getProjectName(), "Project name"))
                .projectCode(trimToNull(project.getProjectCode()))
                .pmName(application == null ? null : trimToNull(application.getMahaitContact()))
                .billedTo(registration == null && project.getDepartment() != null
                        ? trimToNull(project.getDepartment().getDepartmentName()) : resolveBilledTo(registration))
                .billingAddress(resolveBillingAddress(registration))
                .clientGstinAvailable(registration != null && StringUtils.hasText(registration.getGstNo()))
                .clientGstNumber(registration == null ? null : trimToNull(registration.getGstNo()))
                .placeOfSupply(DEFAULT_PLACE_OF_SUPPLY)
                .baseAmount(breakdown.baseAmount())
                .agencyCommissionAmount(totalAgencyCommission)
                .mahaItCommissionAmount(totalMahaItCommission)
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

        TaxInvoiceView view = viewMapper.toView(invoice);
        // Billing editors need the original value, not the shared read-only mapper's masked GST.
        view.setClientGstNumber(invoice.getClientGstNumber());
        view.setPanNumber(invoice.getPanNumber());
        view.setGstNumber(invoice.getGstNumber());
        view.setDocumentTitle("TAX INVOICE");
        List<TaxInvoiceLineItemView> lineItems = new ArrayList<>();
        int lineNumber = 1;
        for (EmployeeLine line : lines) {
            TaxInvoiceLineItemView item = line.lineItem();
            item.setLineNumber(lineNumber++);
            lineItems.add(item);
        }
        view.setLineItems(lineItems);
        return view;
    }

    private EmployeeLine buildEmployeeLine(
            EmployeeEntity employee,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal agencyCommissionMultiplier,
            BigDecimal mahaItCommissionMultiplier) {
        if (employee == null) {
            return null;
        }

        // Bill only the days the employee was actually on the project within the selected period.
        LocalDate from = latest(periodStart, employee.getOnboardingDate());
        LocalDate to = earliest(periodEnd, employee.getResignationDate());
        if (from.isAfter(to)) {
            return null;
        }

        String employeeName = requireText(employee.getFullName(), "Employee name");
        ManpowerDesignationMaster designation = employee.getDesignation();
        if (designation == null || designation.getDesignationId() == null) {
            throw new TaxInvoiceException("Designation is not assigned for employee " + employeeName + ".");
        }
        String levelCode = trimToNull(employee.getLevelCode());
        if (levelCode == null) {
            throw new TaxInvoiceException("Level is not assigned for employee " + employeeName + ".");
        }
        levelCode = levelCode.toUpperCase(Locale.ROOT);

        List<ManpowerDesignationRate> rates = designationRateRepository.findActiveRatesForPeriod(
                designation.getDesignationId(),
                levelCode,
                from,
                to);

        // Prorate per calendar month: full month = full monthly rate, partial month = days worked / days in month.
        BigDecimal manpowerAmount = ZERO;
        BigDecimal latestMonthlyRate = ZERO;
        for (LocalDate segmentStart = from; !segmentStart.isAfter(to);
                segmentStart = segmentStart.withDayOfMonth(1).plusMonths(1)) {
            LocalDate monthEnd = segmentStart.withDayOfMonth(segmentStart.lengthOfMonth());
            LocalDate segmentEnd = monthEnd.isAfter(to) ? to : monthEnd;
            BigDecimal monthlyRate = resolveMonthlyRate(rates, segmentStart, employeeName, designation, levelCode);
            long daysWorked = ChronoUnit.DAYS.between(segmentStart, segmentEnd) + 1;
            manpowerAmount = manpowerAmount.add(monthlyRate
                    .multiply(BigDecimal.valueOf(daysWorked))
                    .divide(BigDecimal.valueOf(segmentStart.lengthOfMonth()), 2, RoundingMode.HALF_UP));
            latestMonthlyRate = monthlyRate;
        }

        BigDecimal agencyCommissionAmount = manpowerAmount.multiply(agencyCommissionMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal subTotal = manpowerAmount.add(agencyCommissionAmount);
        BigDecimal mahaItCommissionAmount = subTotal.multiply(mahaItCommissionMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = subTotal.add(mahaItCommissionAmount).setScale(2, RoundingMode.HALF_UP);

        BigDecimal loadedMonthlyRate = loadRate(latestMonthlyRate, agencyCommissionMultiplier,
                mahaItCommissionMultiplier);

        TaxInvoiceLineItemView lineItem = TaxInvoiceLineItemView.builder()
                .description(buildDescription(employeeName, designation))
                .requiredPeriodDisplay(PERIOD_DATE_FORMATTER.format(from) + " to " + PERIOD_DATE_FORMATTER.format(to))
                .sacHsn(DEFAULT_SAC_HSN)
                .quantity(1)
                .durationInMonths((int) ChronoUnit.MONTHS.between(from.withDayOfMonth(1), to.withDayOfMonth(1)) + 1)
                .ratePerMonth(loadedMonthlyRate)
                .totalAmount(taxableAmount)
                .ratePerMonthDisplay(displayFormatter.formatAmount(loadedMonthlyRate))
                .totalAmountDisplay(displayFormatter.formatAmount(taxableAmount))
                .employeeId(employee.getEmployeeId())
                .employeeCode(trimToNull(employee.getEmployeeCode()))
                .employeeName(employeeName)
                .designationName(trimToNull(designation.getDesignationName()))
                .levelCode(levelCode)
                .billedFrom(from)
                .billedTo(to)
                .build();

        return new EmployeeLine(lineItem, agencyCommissionAmount, mahaItCommissionAmount, taxableAmount);
    }

    private BigDecimal resolveMonthlyRate(
            List<ManpowerDesignationRate> rates,
            LocalDate date,
            String employeeName,
            ManpowerDesignationMaster designation,
            String levelCode) {
        return rates.stream()
                .filter(rate -> rate.getEffectiveFrom() != null && !rate.getEffectiveFrom().isAfter(date))
                .filter(rate -> rate.getEffectiveTo() == null || !rate.getEffectiveTo().isBefore(date))
                .map(ManpowerDesignationRate::getGrossMonthlyCtc)
                .filter(Objects::nonNull)
                .filter(rate -> rate.compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .map(rate -> rate.setScale(2, RoundingMode.HALF_UP))
                .orElseThrow(() -> new TaxInvoiceException(
                        "Designation monthly rate is not configured in master for employee " + employeeName
                                + " (" + designation.getDesignationName() + ", level " + levelCode
                                + ", date " + PERIOD_DATE_FORMATTER.format(date) + ")."));
    }

    private BigDecimal loadRate(
            BigDecimal grossMonthlyRate,
            BigDecimal agencyCommissionMultiplier,
            BigDecimal mahaItCommissionMultiplier) {
        BigDecimal withAgency = grossMonthlyRate.add(grossMonthlyRate.multiply(agencyCommissionMultiplier)
                .setScale(2, RoundingMode.HALF_UP));
        return withAgency.add(withAgency.multiply(mahaItCommissionMultiplier)
                .setScale(2, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildDescription(String employeeName, ManpowerDesignationMaster designation) {
        String designationName = trimToNull(designation.getDesignationName());
        return designationName == null ? employeeName : employeeName + " - " + designationName;
    }

    private DepartmentProjectApplicationEntity resolveProjectApplication(ProjectMst project) {
        if (project.getApplicationId() == null) {
            return null;
        }
        return applicationRepository.findById(project.getApplicationId()).orElse(null);
    }

    private DepartmentRegistrationEntity resolveRegistration(
            ProjectMst project,
            Long departmentId,
            Long subDepartmentId,
            DepartmentProjectApplicationEntity application,
            List<EmployeeProjectMappingEntity> mappings) {
        if (application != null && application.getDepartmentRegistrationId() != null) {
            DepartmentRegistrationEntity registration = registrationRepository
                    .findById(application.getDepartmentRegistrationId())
                    .orElse(null);
            if (registration != null) {
                return registration;
            }
        }

        DepartmentRegistrationEntity employeeRegistration = mappings.stream()
                .map(EmployeeProjectMappingEntity::getEmployee)
                .filter(Objects::nonNull)
                .map(EmployeeEntity::getDepartmentRegistration)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (employeeRegistration != null) {
            return employeeRegistration;
        }

        // Internal projects have no owning department; bill the department selected on the form.
        Long billingDepartmentId = project.getDepartmentId() != null ? project.getDepartmentId() : departmentId;
        Long billingSubDepartmentId = project.getDepartmentId() != null ? project.getSubDepartmentId() : subDepartmentId;
        List<DepartmentRegistrationEntity> departmentRegistrations = billingDepartmentId == null
                ? List.of()
                : registrationRepository.findByDepartmentIdOrderByCreatedAtAsc(billingDepartmentId);
        return departmentRegistrations.stream()
                .filter(registration -> billingSubDepartmentId == null
                        || Objects.equals(registration.getSubDeptId(), billingSubDepartmentId))
                .findFirst()
                .or(() -> departmentRegistrations.stream().findFirst())
                // An unregistered department can supply billing details in the employee invoice preview.
                .orElse(null);
    }

    private String buildReferenceNumber(ProjectMst project, LocalDate periodStart, LocalDate periodEnd) {
        String projectReference = trimToNull(project.getProjectCode());
        if (projectReference == null) {
            projectReference = "P" + project.getProjectId();
        }
        return "TI-" + projectReference.toUpperCase(Locale.ROOT)
                + "-" + REFERENCE_DATE_FORMATTER.format(periodStart)
                + "-" + REFERENCE_DATE_FORMATTER.format(periodEnd);
    }

    private BigDecimal resolveCommissionMultiplier(String type) {
        BigDecimal percentage = rateMasterRepository.findByTypeIgnoreCase(type)
                .filter(rate -> "Y".equalsIgnoreCase(trimToNull(rate.getActiveFlag())))
                .map(RateMaster::getRate)
                .orElse(new BigDecimal("10.00"));
        return percentage.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
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

    private String resolveBilledTo(DepartmentRegistrationEntity registration) {
        if (registration == null) {
            return "";
        }
        String billedTo = trimToNull(registration.getBillDepartmentName());
        if (billedTo == null) {
            billedTo = trimToNull(registration.getDepartmentName());
        }
        return billedTo != null ? billedTo : "";
    }

    private String resolveBillingAddress(DepartmentRegistrationEntity registration) {
        if (registration == null) {
            return "";
        }
        String billingAddress = trimToNull(registration.getBillAddress());
        if (billingAddress == null) {
            billingAddress = trimToNull(registration.getAddress());
        }
        return billingAddress != null ? billingAddress : "";
    }

    private LocalDate latest(LocalDate first, LocalDate second) {
        return second != null && second.isAfter(first) ? second : first;
    }

    private LocalDate earliest(LocalDate first, LocalDate second) {
        return second != null && second.isBefore(first) ? second : first;
    }

    private BigDecimal sum(List<BigDecimal> values) {
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new TaxInvoiceException(fieldName + " is required.");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record EmployeeLine(
            TaxInvoiceLineItemView lineItem,
            BigDecimal agencyCommissionAmount,
            BigDecimal mahaItCommissionAmount,
            BigDecimal taxableAmount) {
    }
}
