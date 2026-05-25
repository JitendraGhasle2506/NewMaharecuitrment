package com.maharecruitment.gov.in.invoice.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.attendance.dto.AttendanceReportDTO;
import com.maharecruitment.gov.in.attendance.service.AttendanceRegisterService;
import com.maharecruitment.gov.in.attendance.service.InternalEmployeeAttendanceReportService;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillGenerateRequest;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillListItemView;
import com.maharecruitment.gov.in.invoice.dto.AgencyMonthlyBillView;
import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillEmployeeType;
import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillEntity;
import com.maharecruitment.gov.in.invoice.entity.AgencyMonthlyBillLineItemEntity;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceException;
import com.maharecruitment.gov.in.invoice.exception.TaxInvoiceNotFoundException;
import com.maharecruitment.gov.in.invoice.repository.AgencyMonthlyBillRepository;
import com.maharecruitment.gov.in.invoice.service.AgencyMonthlyBillNumberGenerator;
import com.maharecruitment.gov.in.invoice.service.AgencyMonthlyBillService;
import com.maharecruitment.gov.in.invoice.service.AgencyMonthlyBillViewMapper;
import com.maharecruitment.gov.in.master.entity.AgencyMaster;
import com.maharecruitment.gov.in.master.entity.CommissionCode;
import com.maharecruitment.gov.in.master.entity.CommissionRateMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationMaster;
import com.maharecruitment.gov.in.master.entity.ManpowerDesignationRate;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;
import com.maharecruitment.gov.in.master.repository.CommissionRateMasterRepository;
import com.maharecruitment.gov.in.master.repository.ManpowerDesignationRateRepository;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class AgencyMonthlyBillServiceImpl implements AgencyMonthlyBillService {

    private static final Logger log = LoggerFactory.getLogger(AgencyMonthlyBillServiceImpl.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String DEFAULT_ACTOR = "SYSTEM";

    private final AgencyMonthlyBillRepository billRepository;
    private final AgencyMonthlyBillNumberGenerator billNumberGenerator;
    private final AgencyMonthlyBillViewMapper viewMapper;
    private final AttendanceRegisterService attendanceRegisterService;
    private final InternalEmployeeAttendanceReportService internalAttendanceReportService;
    private final EmployeeRepository employeeRepository;
    private final AgencyMasterRepository agencyMasterRepository;
    private final ManpowerDesignationRateRepository designationRateRepository;
    private final CommissionRateMasterRepository commissionRateRepository;

    public AgencyMonthlyBillServiceImpl(
            AgencyMonthlyBillRepository billRepository,
            AgencyMonthlyBillNumberGenerator billNumberGenerator,
            AgencyMonthlyBillViewMapper viewMapper,
            AttendanceRegisterService attendanceRegisterService,
            InternalEmployeeAttendanceReportService internalAttendanceReportService,
            EmployeeRepository employeeRepository,
            AgencyMasterRepository agencyMasterRepository,
            ManpowerDesignationRateRepository designationRateRepository,
            CommissionRateMasterRepository commissionRateRepository) {
        this.billRepository = billRepository;
        this.billNumberGenerator = billNumberGenerator;
        this.viewMapper = viewMapper;
        this.attendanceRegisterService = attendanceRegisterService;
        this.internalAttendanceReportService = internalAttendanceReportService;
        this.employeeRepository = employeeRepository;
        this.agencyMasterRepository = agencyMasterRepository;
        this.designationRateRepository = designationRateRepository;
        this.commissionRateRepository = commissionRateRepository;
    }

    @Override
    public Page<AgencyMonthlyBillListItemView> getGeneratedBills(Pageable pageable) {
        Pageable resolvedPageable = pageable != null
                ? pageable
                : PageRequest.of(
                        0,
                        10,
                        Sort.by(Sort.Order.desc("generatedDate"), Sort.Order.desc("agencyMonthlyBillId")));
        return billRepository.findByActiveTrue(resolvedPageable).map(viewMapper::toListItemView);
    }

    @Override
    public AgencyMonthlyBillView getBill(Long billId) {
        if (billId == null) {
            throw new TaxInvoiceException("Bill id is required.");
        }
        return billRepository.findDetailedByAgencyMonthlyBillIdAndActiveTrue(billId)
                .map(viewMapper::toView)
                .orElseThrow(() -> new TaxInvoiceNotFoundException("Agency monthly bill not found for id: " + billId));
    }

    @Override
    public AgencyMonthlyBillView preview(AgencyMonthlyBillGenerateRequest request) {
        AgencyMonthlyBillEntity preview = buildBill(request, "PREVIEW", DEFAULT_ACTOR, false, null);
        return viewMapper.toView(preview);
    }

    @Override
    @Transactional
    public AgencyMonthlyBillView generate(AgencyMonthlyBillGenerateRequest request, String actorEmail) {
        validateRequest(request);
        LocalDate generatedDate = LocalDate.now();
        String billNumber = billNumberGenerator.generate(
                request.getAgencyId(),
                request.getYear(),
                request.getMonth(),
                generatedDate);
        AgencyMonthlyBillEntity bill = buildBill(request, billNumber, actorEmail, true, null);

        AgencyMonthlyBillEntity saved = billRepository.save(bill);
        log.info(
                "Agency monthly bill generated. billId={}, billNumber={}, agencyId={}, month={}, year={}, total={}",
                saved.getAgencyMonthlyBillId(),
                saved.getBillNumber(),
                saved.getAgencyId(),
                saved.getBillMonth(),
                saved.getBillYear(),
                saved.getTotalAmount());
        return getBill(saved.getAgencyMonthlyBillId());
    }

    @Override
    @Transactional
    public void softDelete(Long billId, String actorEmail) {
        if (billId == null) {
            throw new TaxInvoiceException("Bill id is required.");
        }
        AgencyMonthlyBillEntity bill = billRepository.findById(billId)
                .orElseThrow(() -> new TaxInvoiceNotFoundException("Agency monthly bill not found for id: " + billId));
        if (Boolean.FALSE.equals(bill.getActive())) {
            throw new TaxInvoiceException("Agency monthly bill is already deleted.");
        }
        bill.setActive(Boolean.FALSE);
        applyUpdateAuditMetadata(bill, actorEmail);
        billRepository.save(bill);
        log.info(
                "Agency monthly bill soft deleted. billId={}, billNumber={}, agencyId={}, month={}, year={}",
                bill.getAgencyMonthlyBillId(),
                bill.getBillNumber(),
                bill.getAgencyId(),
                bill.getBillMonth(),
                bill.getBillYear());
    }

    private AgencyMonthlyBillEntity buildBill(
            AgencyMonthlyBillGenerateRequest request,
            String billNumber,
            String actorEmail,
            boolean persistable,
            Long excludedBillId) {
        validateRequest(request);
        YearMonth billPeriod = YearMonth.of(request.getYear(), request.getMonth());
        LocalDate periodFrom = billPeriod.atDay(1);
        LocalDate periodTo = billPeriod.atEndOfMonth();
        int daysInMonth = billPeriod.lengthOfMonth();
        AgencyMonthlyBillEmployeeType employeeType = resolveEmployeeType(request);

        AgencyMaster agency = agencyMasterRepository.findById(request.getAgencyId())
                .orElseThrow(() -> new TaxInvoiceException("Agency not found for id: " + request.getAgencyId()));
        BigDecimal agencyMarginRate = resolveAgencyMarginRate(periodTo);

        List<BillAttendanceRow> attendanceRows = loadAttendanceRows(request, employeeType);
        if (attendanceRows == null || attendanceRows.isEmpty()) {
            throw new TaxInvoiceException("No attendance records found for selected agency, employee type and month.");
        }
        List<BillAttendanceRow> billableAttendanceRows = excludeAlreadyBilledEmployees(
                request,
                attendanceRows,
                excludedBillId);
        if (billableAttendanceRows.isEmpty()) {
            throw new TaxInvoiceException(
                    "All eligible employees for the selected agency and month are already included in an active bill.");
        }

        Map<Long, EmployeeEntity> employeesById = loadEmployees(billableAttendanceRows);
        List<AgencyMonthlyBillLineItemEntity> lineItems = buildLineItems(
                billableAttendanceRows,
                employeesById,
                daysInMonth,
                periodFrom,
                periodTo,
                agencyMarginRate);
        if (lineItems.isEmpty()) {
            throw new TaxInvoiceException("No billable employees found for selected agency and month.");
        }

        BigDecimal attendanceAmount = sum(lineItems.stream()
                .map(AgencyMonthlyBillLineItemEntity::getAttendanceAmount)
                .toList());
        BigDecimal agencyMarginAmount = sum(lineItems.stream()
                .map(AgencyMonthlyBillLineItemEntity::getAgencyMarginAmount)
                .toList());
        BigDecimal totalAmount = attendanceAmount.add(agencyMarginAmount).setScale(2, RoundingMode.HALF_UP);
        if (totalAmount.compareTo(ZERO_AMOUNT) <= 0) {
            throw new TaxInvoiceException("Bill total must be greater than zero.");
        }

        AgencyMonthlyBillEntity bill = AgencyMonthlyBillEntity.builder()
                .billNumber(requireText(billNumber, "Bill number"))
                .agencyId(agency.getAgencyId())
                .agencyName(requireText(agency.getAgencyName(), "Agency name"))
                .billYear(request.getYear())
                .billMonth(request.getMonth())
                .employeeType(employeeType.name())
                .generatedDate(LocalDate.now())
                .periodFrom(periodFrom)
                .periodTo(periodTo)
                .daysInMonth(daysInMonth)
                .employeeCount(lineItems.size())
                .agencyMarginRate(agencyMarginRate)
                .attendanceAmount(attendanceAmount)
                .agencyMarginAmount(agencyMarginAmount)
                .totalAmount(totalAmount)
                .active(Boolean.TRUE)
                .build();
        bill.replaceLineItems(lineItems);
        if (persistable) {
            applyAuditMetadata(bill, actorEmail);
        }
        return bill;
    }

    private List<BillAttendanceRow> excludeAlreadyBilledEmployees(
            AgencyMonthlyBillGenerateRequest request,
            List<BillAttendanceRow> attendanceRows,
            Long excludedBillId) {
        Set<Long> billedEmployeeIds = findAlreadyBilledEmployeeIds(request, excludedBillId);
        if (billedEmployeeIds.isEmpty()) {
            return attendanceRows;
        }
        return attendanceRows.stream()
                .filter(row -> row != null
                        && row.employeeId() != null
                        && !billedEmployeeIds.contains(row.employeeId()))
                .toList();
    }

    private Set<Long> findAlreadyBilledEmployeeIds(
            AgencyMonthlyBillGenerateRequest request,
            Long excludedBillId) {
        List<Long> employeeIds = billRepository.findBilledEmployeeIdsForPeriod(
                request.getAgencyId(),
                request.getYear(),
                request.getMonth(),
                excludedBillId);
        if (employeeIds == null || employeeIds.isEmpty()) {
            return Set.of();
        }
        return employeeIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<BillAttendanceRow> loadAttendanceRows(
            AgencyMonthlyBillGenerateRequest request,
            AgencyMonthlyBillEmployeeType employeeType) {
        Map<Long, BillAttendanceRow> rowsByEmployee = new LinkedHashMap<>();
        if (employeeType.includesExternal()) {
            loadExternalAttendanceRows(request).forEach(row -> rowsByEmployee.putIfAbsent(row.employeeId(), row));
        }
        if (employeeType.includesInternal()) {
            loadInternalAttendanceRows(request).forEach(row -> rowsByEmployee.put(row.employeeId(), row));
        }
        return new ArrayList<>(rowsByEmployee.values());
    }

    private List<BillAttendanceRow> loadExternalAttendanceRows(AgencyMonthlyBillGenerateRequest request) {
        List<AttendanceReportDTO> rows = attendanceRegisterService.getExternalAttendanceReportData(
                null,
                request.getAgencyId(),
                request.getMonth(),
                request.getYear(),
                null);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null && row.getUserId() != null)
                .map(row -> new BillAttendanceRow(
                        row.getUserId(),
                        row.getRequestId(),
                        row.getEmployeeName(),
                        row.getDesignation(),
                        AgencyMonthlyBillEmployeeType.EXTERNAL,
                        row.getDailyStatus(),
                        row.getPresentCount(),
                        row.getAbsentCount(),
                        row.getLeaveCount(),
                        row.getCompOffCount(),
                        row.getTourCount(),
                        row.getHolidayCount(),
                        row.getWeekOffCount()))
                .toList();
    }

    private List<BillAttendanceRow> loadInternalAttendanceRows(AgencyMonthlyBillGenerateRequest request) {
        InternalAttendanceReportFilter filter = new InternalAttendanceReportFilter();
        filter.setAgencyId(request.getAgencyId());
        filter.setMonth(request.getMonth());
        filter.setYear(request.getYear());
        filter.setEmployeeStatus("ACTIVE");

        InternalAttendanceReportView report = internalAttendanceReportService.buildReport(filter);
        if (report == null || report.getRows() == null || report.getRows().isEmpty()) {
            return List.of();
        }
        return report.getRows().stream()
                .filter(row -> row != null && row.getEmployeeId() != null)
                .map(row -> new BillAttendanceRow(
                        row.getEmployeeId(),
                        row.getRequestId(),
                        row.getEmployeeName(),
                        row.getDesignation(),
                        AgencyMonthlyBillEmployeeType.INTERNAL,
                        row.getDailyStatus(),
                        row.getPresentCount(),
                        row.getAbsentCount(),
                        row.getLeaveCount(),
                        row.getCompOffCount(),
                        row.getTourCount(),
                        row.getHolidayCount(),
                        row.getWeekOffCount()))
                .toList();
    }

    private List<AgencyMonthlyBillLineItemEntity> buildLineItems(
            List<BillAttendanceRow> attendanceRows,
            Map<Long, EmployeeEntity> employeesById,
            int daysInMonth,
            LocalDate periodFrom,
            LocalDate periodTo,
            BigDecimal agencyMarginRate) {
        List<AgencyMonthlyBillLineItemEntity> lineItems = new ArrayList<>();
        Map<RateLookupKey, BigDecimal> monthlyRateCache = new LinkedHashMap<>();
        Map<Long, List<ManpowerDesignationRate>> designationRatesCache = new LinkedHashMap<>();
        int lineNumber = 1;
        for (BillAttendanceRow row : attendanceRows) {
            if (row == null || row.employeeId() == null) {
                continue;
            }

            EmployeeEntity employee = employeesById.get(row.employeeId());
            if (employee == null) {
                throw new TaxInvoiceException("Employee profile not found for id: " + row.employeeId());
            }
            if (!matchesEmployeeType(employee, row.employeeType())) {
                continue;
            }

            ManpowerDesignationMaster designation = employee.getDesignation();
            if (designation == null || designation.getDesignationId() == null) {
                throw new TaxInvoiceException("Designation is not mapped for employee: " + employee.getFullName());
            }
            String levelCode = requireText(employee.getLevelCode(), "Level code for employee " + employee.getFullName());
            BigDecimal monthlyRate = resolveMonthlyRate(
                    designation.getDesignationId(),
                    levelCode,
                    periodFrom,
                    periodTo,
                    employee.getFullName(),
                    monthlyRateCache,
                    designationRatesCache);
            long presentDays = resolveStatusCount(row.dailyStatus(), "P", row.presentCount());
            long absentDays = resolveStatusCount(row.dailyStatus(), "A", row.absentCount());
            long leaveDays = resolveStatusCount(row.dailyStatus(), "L", row.leaveCount());
            long compOffDays = resolveStatusCount(row.dailyStatus(), "CO", row.compOffCount());
            long tourDays = resolveStatusCount(row.dailyStatus(), "T", row.tourCount());
            long holidayDays = resolveStatusCount(row.dailyStatus(), "H", row.holidayCount());
            long weekOffDays = resolveStatusCount(row.dailyStatus(), "W", row.weekOffCount());
            long payableDays = presentDays + compOffDays + tourDays + holidayDays + weekOffDays;
            BigDecimal attendanceAmount = calculateAttendanceAmount(monthlyRate, daysInMonth, payableDays);
            BigDecimal marginAmount = calculatePercentage(attendanceAmount, agencyMarginRate);

            lineItems.add(AgencyMonthlyBillLineItemEntity.builder()
                    .lineNumber(lineNumber++)
                    .employeeId(employee.getEmployeeId())
                    .employeeCode(trimToNull(employee.getEmployeeCode()))
                    .requestId(trimToNull(employee.getRequestId()))
                    .employeeName(requireText(employee.getFullName(), "Employee name"))
                    .employeeType(row.employeeType().name())
                    .designationId(designation.getDesignationId())
                    .designationName(resolveDesignationName(designation, row))
                    .levelCode(levelCode)
                    .monthlyRate(monthlyRate)
                    .daysInMonth(daysInMonth)
                    .payableDays(payableDays)
                    .presentDays(presentDays)
                    .absentDays(absentDays)
                    .leaveDays(leaveDays)
                    .compOffDays(compOffDays)
                    .tourDays(tourDays)
                    .holidayDays(holidayDays)
                    .weekOffDays(weekOffDays)
                    .attendanceAmount(attendanceAmount)
                    .agencyMarginRate(agencyMarginRate)
                    .agencyMarginAmount(marginAmount)
                    .lineTotal(attendanceAmount.add(marginAmount).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }
        return lineItems;
    }

    private boolean matchesEmployeeType(EmployeeEntity employee, AgencyMonthlyBillEmployeeType sourceType) {
        if (employee == null || !StringUtils.hasText(employee.getRecruitmentType())) {
            return true;
        }
        return sourceType.name().equalsIgnoreCase(employee.getRecruitmentType().trim());
    }

    private long resolveStatusCount(Map<Integer, String> dailyStatus, String statusCode, long fallbackCount) {
        if (dailyStatus == null || dailyStatus.isEmpty()) {
            return fallbackCount;
        }
        return dailyStatus.values().stream()
                .filter(status -> statusCode.equalsIgnoreCase(trimToNull(status)))
                .count();
    }

    private Map<Long, EmployeeEntity> loadEmployees(List<BillAttendanceRow> attendanceRows) {
        List<Long> employeeIds = attendanceRows.stream()
                .map(BillAttendanceRow::employeeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (employeeIds.isEmpty()) {
            return Map.of();
        }
        return employeeRepository.findDetailedByEmployeeIdIn(employeeIds)
                .stream()
                .collect(Collectors.toMap(
                        EmployeeEntity::getEmployeeId,
                        employee -> employee,
                        (first, second) -> first,
                        LinkedHashMap::new));
    }

    private BigDecimal resolveMonthlyRate(
            Long designationId,
            String levelCode,
            LocalDate periodFrom,
            LocalDate periodTo,
            String employeeName,
            Map<RateLookupKey, BigDecimal> monthlyRateCache,
            Map<Long, List<ManpowerDesignationRate>> designationRatesCache) {
        String normalizedLevelCode = normalizeLevelCode(levelCode);
        String levelLookupKey = normalizeLevelLookupKey(normalizedLevelCode);
        RateLookupKey key = new RateLookupKey(designationId, levelLookupKey, periodFrom, periodTo);
        BigDecimal cachedRate = monthlyRateCache.get(key);
        if (cachedRate != null) {
            return cachedRate;
        }

        ManpowerDesignationRate matchingRate = designationRateRepository
                .findActiveRatesForPeriod(designationId, normalizedLevelCode, periodFrom, periodTo)
                .stream()
                .findFirst()
                .orElseGet(() -> resolveBestMatchingRate(
                        designationId,
                        normalizedLevelCode,
                        periodFrom,
                        periodTo,
                        designationRatesCache));
        if (matchingRate == null) {
            throw new TaxInvoiceException(buildMonthlyRateNotFoundMessage(
                    designationId,
                    normalizedLevelCode,
                    periodFrom,
                    periodTo,
                    employeeName,
                    designationRatesCache));
        }

        BigDecimal monthlyRate = normalizeCurrency(matchingRate.getGrossMonthlyCtc());
        monthlyRateCache.put(key, monthlyRate);
        return monthlyRate;
    }

    private ManpowerDesignationRate resolveBestMatchingRate(
            Long designationId,
            String levelCode,
            LocalDate periodFrom,
            LocalDate periodTo,
            Map<Long, List<ManpowerDesignationRate>> designationRatesCache) {
        List<ManpowerDesignationRate> matchingRates = findRatesByNormalizedLevel(
                designationId,
                levelCode,
                designationRatesCache);
        return matchingRates.stream()
                .filter(this::isActiveRate)
                .filter(rate -> overlapsBillPeriod(rate, periodFrom, periodTo))
                .findFirst()
                .orElseGet(() -> matchingRates.stream()
                        .filter(this::isActiveRate)
                        .findFirst()
                        .orElse(null));
    }

    private String buildMonthlyRateNotFoundMessage(
            Long designationId,
            String levelCode,
            LocalDate periodFrom,
            LocalDate periodTo,
            String employeeName,
            Map<Long, List<ManpowerDesignationRate>> designationRatesCache) {
        List<ManpowerDesignationRate> matchingRates = findRatesByNormalizedLevel(
                designationId,
                levelCode,
                designationRatesCache);
        String baseMessage = "Designation monthly rate is not configured for employee " + employeeName
                + " during bill period " + periodFrom + " to " + periodTo
                + " (designationId=" + designationId + ", levelCode=" + levelCode + ").";
        if (matchingRates.isEmpty()) {
            String availableLevels = loadRatesByDesignation(designationId, designationRatesCache)
                    .stream()
                    .map(ManpowerDesignationRate::getLevelCode)
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(10)
                    .collect(Collectors.joining(", "));
            return StringUtils.hasText(availableLevels)
                    ? baseMessage + " Available level codes for this designation: " + availableLevels + "."
                    : baseMessage;
        }

        boolean activeRateExists = matchingRates.stream()
                .anyMatch(rate -> "Y".equalsIgnoreCase(trimToNull(rate.getActiveFlag())));
        if (!activeRateExists) {
            return baseMessage + " Matching rate exists but is inactive.";
        }

        String availableRanges = matchingRates.stream()
                .filter(this::isActiveRate)
                .limit(5)
                .map(this::formatRateRange)
                .collect(Collectors.joining(", "));
        return baseMessage + " Available active ranges: " + availableRanges + ".";
    }

    private List<ManpowerDesignationRate> findRatesByNormalizedLevel(
            Long designationId,
            String levelCode,
            Map<Long, List<ManpowerDesignationRate>> designationRatesCache) {
        String targetLevelKey = normalizeLevelLookupKey(levelCode);
        if (designationId == null || !StringUtils.hasText(targetLevelKey)) {
            return List.of();
        }
        return loadRatesByDesignation(designationId, designationRatesCache)
                .stream()
                .filter(rate -> targetLevelKey.equals(normalizeLevelLookupKey(rate.getLevelCode())))
                .toList();
    }

    private List<ManpowerDesignationRate> loadRatesByDesignation(
            Long designationId,
            Map<Long, List<ManpowerDesignationRate>> designationRatesCache) {
        if (designationId == null) {
            return List.of();
        }
        return designationRatesCache.computeIfAbsent(
                designationId,
                designationRateRepository::findByDesignationIdOrderByEffectiveFromDesc);
    }

    private boolean isActiveRate(ManpowerDesignationRate rate) {
        return rate != null && "Y".equalsIgnoreCase(trimToNull(rate.getActiveFlag()));
    }

    private boolean overlapsBillPeriod(ManpowerDesignationRate rate, LocalDate periodFrom, LocalDate periodTo) {
        if (rate == null || rate.getEffectiveFrom() == null || periodFrom == null || periodTo == null) {
            return false;
        }
        return !rate.getEffectiveFrom().isAfter(periodTo)
                && (rate.getEffectiveTo() == null || !rate.getEffectiveTo().isBefore(periodFrom));
    }

    private String formatRateRange(ManpowerDesignationRate rate) {
        String activeStatus = "Y".equalsIgnoreCase(trimToNull(rate.getActiveFlag())) ? "active" : "inactive";
        String effectiveTo = rate.getEffectiveTo() == null ? "open" : rate.getEffectiveTo().toString();
        return "[" + rate.getEffectiveFrom() + " to " + effectiveTo + ", " + activeStatus + "]";
    }

    private String normalizeLevelCode(String levelCode) {
        return requireText(levelCode, "Level code").toUpperCase(Locale.ROOT);
    }

    private String normalizeLevelLookupKey(String levelCode) {
        String normalized = trimToNull(levelCode);
        if (normalized == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char character = Character.toUpperCase(normalized.charAt(i));
            if (Character.isLetterOrDigit(character)) {
                builder.append(character);
            }
        }
        String lookupKey = builder.toString();
        if (lookupKey.startsWith("LEVEL") && lookupKey.length() > 5) {
            lookupKey = "L" + lookupKey.substring(5);
        }
        if (lookupKey.length() > 2 && lookupKey.charAt(0) == 'L' && isDigits(lookupKey.substring(1))) {
            return "L" + stripLeadingZeroes(lookupKey.substring(1));
        }
        return lookupKey;
    }

    private boolean isDigits(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String stripLeadingZeroes(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }

    private BigDecimal resolveAgencyMarginRate(LocalDate effectiveDate) {
        return commissionRateRepository.findApplicableActiveRates(CommissionCode.AGENCY, effectiveDate)
                .stream()
                .findFirst()
                .map(CommissionRateMaster::getCommissionPercentage)
                .map(rate -> rate.setScale(4, RoundingMode.HALF_UP))
                .orElseThrow(() -> new TaxInvoiceException(
                        "Active Agency commission rate is not configured for " + effectiveDate + "."));
    }

    private BigDecimal calculateAttendanceAmount(BigDecimal monthlyRate, int daysInMonth, long payableDays) {
        if (payableDays <= 0) {
            return ZERO_AMOUNT;
        }
        return monthlyRate
                .divide(BigDecimal.valueOf(daysInMonth), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(payableDays))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal percentage) {
        if (amount == null || percentage == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ZERO_AMOUNT;
        }
        return amount.multiply(percentage)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeCurrency(BigDecimal amount) {
        if (amount == null) {
            return ZERO_AMOUNT;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveDesignationName(ManpowerDesignationMaster designation, BillAttendanceRow row) {
        if (StringUtils.hasText(designation.getDesignationName())) {
            return designation.getDesignationName().trim();
        }
        return requireText(row.designation(), "Designation name");
    }

    private void validateRequest(AgencyMonthlyBillGenerateRequest request) {
        if (request == null) {
            throw new TaxInvoiceException("Bill generation request is required.");
        }
        if (request.getAgencyId() == null) {
            throw new TaxInvoiceException("Agency is required.");
        }
        if (request.getMonth() == null || request.getMonth() < 1 || request.getMonth() > 12) {
            throw new TaxInvoiceException("Month must be between 1 and 12.");
        }
        if (request.getYear() == null || request.getYear() < 2000 || request.getYear() > 2100) {
            throw new TaxInvoiceException("Year must be valid.");
        }
        if (request.getEmployeeType() == null) {
            throw new TaxInvoiceException("Employee type is required.");
        }
    }

    private AgencyMonthlyBillEmployeeType resolveEmployeeType(AgencyMonthlyBillGenerateRequest request) {
        return request.getEmployeeType() == null
                ? AgencyMonthlyBillEmployeeType.ALL
                : request.getEmployeeType();
    }

    private void applyAuditMetadata(AgencyMonthlyBillEntity bill, String actorEmail) {
        String actor = resolveActor(actorEmail);
        LocalDateTime now = LocalDateTime.now();
        bill.setCreatedBy(actor);
        bill.setCreatedDate(now);
        bill.setUpdatedBy(actor);
        bill.setUpdatedDate(now);
    }

    private void copyRecalculatedValues(
            AgencyMonthlyBillEntity target,
            AgencyMonthlyBillEntity source,
            String actorEmail) {
        target.setAgencyName(source.getAgencyName());
        target.setBillYear(source.getBillYear());
        target.setBillMonth(source.getBillMonth());
        target.setEmployeeType(source.getEmployeeType());
        target.setGeneratedDate(source.getGeneratedDate());
        target.setPeriodFrom(source.getPeriodFrom());
        target.setPeriodTo(source.getPeriodTo());
        target.setDaysInMonth(source.getDaysInMonth());
        target.setEmployeeCount(source.getEmployeeCount());
        target.setAgencyMarginRate(source.getAgencyMarginRate());
        target.setAttendanceAmount(source.getAttendanceAmount());
        target.setAgencyMarginAmount(source.getAgencyMarginAmount());
        target.setTotalAmount(source.getTotalAmount());
        target.setActive(Boolean.TRUE);
        replaceLineItemsInPlace(target, source.getLineItems());
        applyUpdateAuditMetadata(target, actorEmail);
    }

    private void replaceLineItemsInPlace(
            AgencyMonthlyBillEntity target,
            List<AgencyMonthlyBillLineItemEntity> sourceItems) {
        List<AgencyMonthlyBillLineItemEntity> targetItems = target.getLineItems();
        List<AgencyMonthlyBillLineItemEntity> safeSourceItems = sourceItems == null ? List.of() : sourceItems;
        int commonSize = Math.min(targetItems.size(), safeSourceItems.size());

        for (int i = 0; i < commonSize; i++) {
            copyLineItemValues(targetItems.get(i), safeSourceItems.get(i));
        }
        for (int i = targetItems.size() - 1; i >= safeSourceItems.size(); i--) {
            targetItems.remove(i);
        }
        for (int i = commonSize; i < safeSourceItems.size(); i++) {
            target.addLineItem(safeSourceItems.get(i));
        }
    }

    private void copyLineItemValues(
            AgencyMonthlyBillLineItemEntity target,
            AgencyMonthlyBillLineItemEntity source) {
        target.setLineNumber(source.getLineNumber());
        target.setEmployeeId(source.getEmployeeId());
        target.setEmployeeCode(source.getEmployeeCode());
        target.setRequestId(source.getRequestId());
        target.setEmployeeName(source.getEmployeeName());
        target.setEmployeeType(source.getEmployeeType());
        target.setDesignationId(source.getDesignationId());
        target.setDesignationName(source.getDesignationName());
        target.setLevelCode(source.getLevelCode());
        target.setMonthlyRate(source.getMonthlyRate());
        target.setDaysInMonth(source.getDaysInMonth());
        target.setPayableDays(source.getPayableDays());
        target.setPresentDays(source.getPresentDays());
        target.setAbsentDays(source.getAbsentDays());
        target.setLeaveDays(source.getLeaveDays());
        target.setCompOffDays(source.getCompOffDays());
        target.setTourDays(source.getTourDays());
        target.setHolidayDays(source.getHolidayDays());
        target.setWeekOffDays(source.getWeekOffDays());
        target.setAttendanceAmount(source.getAttendanceAmount());
        target.setAgencyMarginRate(source.getAgencyMarginRate());
        target.setAgencyMarginAmount(source.getAgencyMarginAmount());
        target.setLineTotal(source.getLineTotal());
    }

    private void applyUpdateAuditMetadata(AgencyMonthlyBillEntity bill, String actorEmail) {
        String actor = resolveActor(actorEmail);
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(bill.getCreatedBy())) {
            bill.setCreatedBy(actor);
        }
        if (bill.getCreatedDate() == null) {
            bill.setCreatedDate(now);
        }
        bill.setUpdatedBy(actor);
        bill.setUpdatedDate(now);
    }

    private String resolveActor(String actorEmail) {
        return StringUtils.hasText(actorEmail) ? actorEmail.trim() : DEFAULT_ACTOR;
    }

    private String requireText(String value, String label) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new TaxInvoiceException(label + " is required.");
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

    private record BillAttendanceRow(
            Long employeeId,
            String requestId,
            String employeeName,
            String designation,
            AgencyMonthlyBillEmployeeType employeeType,
            Map<Integer, String> dailyStatus,
            long presentCount,
            long absentCount,
            long leaveCount,
            long compOffCount,
            long tourCount,
            long holidayCount,
            long weekOffCount) {
    }

    private record RateLookupKey(
            Long designationId,
            String levelCode,
            LocalDate periodFrom,
            LocalDate periodTo) {
    }
}
