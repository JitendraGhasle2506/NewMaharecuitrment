package com.maharecruitment.gov.in.web.controller.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.maharecruitment.gov.in.department.service.ApprovedPaymentReportService;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportFilter;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportRowView;
import com.maharecruitment.gov.in.department.service.model.ApprovedPaymentReportSummaryView;
import com.maharecruitment.gov.in.department.service.model.DepartmentPaymentReportDepartmentOptionView;
import com.maharecruitment.gov.in.web.dto.admin.AdminApprovedPaymentReportFilterForm;
import com.maharecruitment.gov.in.web.service.admin.AdminApprovedPaymentReportCsvExporter;
import com.maharecruitment.gov.in.web.service.admin.AdminApprovedPaymentReportPdfExporter;
import com.maharecruitment.gov.in.web.service.admin.model.AdminApprovedPaymentReportPdfData;

@Controller
@RequestMapping("/admin/reports/payments")
public class AdminPaymentReportPageController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ApprovedPaymentReportService approvedPaymentReportService;
    private final AdminApprovedPaymentReportCsvExporter csvExporter;
    private final AdminApprovedPaymentReportPdfExporter pdfExporter;

    public AdminPaymentReportPageController(
            ApprovedPaymentReportService approvedPaymentReportService,
            AdminApprovedPaymentReportCsvExporter csvExporter,
            AdminApprovedPaymentReportPdfExporter pdfExporter) {
        this.approvedPaymentReportService = approvedPaymentReportService;
        this.csvExporter = csvExporter;
        this.pdfExporter = pdfExporter;
    }

    @GetMapping
    public String view(
            @ModelAttribute("reportFilter") AdminApprovedPaymentReportFilterForm filterForm,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) Integer size,
            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size));
        ApprovedPaymentReportFilter filter = buildFilter(filterForm);

        Page<ApprovedPaymentReportRowView> payments = approvedPaymentReportService.getApprovedPayments(filter, pageable);
        ApprovedPaymentReportSummaryView summary = approvedPaymentReportService.getApprovedPaymentSummary(filter);
        List<DepartmentPaymentReportDepartmentOptionView> departments = approvedPaymentReportService.getDepartmentOptions();

        model.addAttribute("payments", payments);
        model.addAttribute("reportSummary", summary);
        model.addAttribute("departmentOptions", departments);
        model.addAttribute("financialYearOptions", buildFinancialYearOptions());
        model.addAttribute("selectedDepartmentName", resolveDepartmentName(filterForm.getDepartmentRegistrationId(), departments));
        model.addAttribute("selectedFinancialYearLabel", hasText(filterForm.getFinancialYear()) ? filterForm.getFinancialYear() : "All");
        model.addAttribute("approvedDateRangeLabel", buildApprovedDateRangeLabel(filterForm));
        model.addAttribute("generatedAt", LocalDateTime.now());
        model.addAttribute("pageTitle", "Approved Payment Reports");
        return "admin/reports/payment-report-list";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @ModelAttribute("reportFilter") AdminApprovedPaymentReportFilterForm filterForm) {
        ApprovedPaymentReportFilter filter = buildFilter(filterForm);
        byte[] csv = csvExporter.export(approvedPaymentReportService.getApprovedPaymentsForExport(filter));

        String fileName = "approved-payment-report-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @ModelAttribute("reportFilter") AdminApprovedPaymentReportFilterForm filterForm) {
        ApprovedPaymentReportFilter filter = buildFilter(filterForm);
        List<ApprovedPaymentReportRowView> rows = approvedPaymentReportService.getApprovedPaymentsForExport(filter);
        ApprovedPaymentReportSummaryView summary = approvedPaymentReportService.getApprovedPaymentSummary(filter);
        List<DepartmentPaymentReportDepartmentOptionView> departments = approvedPaymentReportService.getDepartmentOptions();
        LocalDateTime generatedAt = LocalDateTime.now();

        byte[] pdf = pdfExporter.export(new AdminApprovedPaymentReportPdfData(
                "Approved Payment Register",
                resolveDepartmentName(filterForm.getDepartmentRegistrationId(), departments),
                hasText(filterForm.getFinancialYear()) ? filterForm.getFinancialYear() : "All",
                buildApprovedDateRangeLabel(filterForm),
                generatedAt,
                summary,
                rows));

        String fileName = "approved-payment-report-" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private ApprovedPaymentReportFilter buildFilter(AdminApprovedPaymentReportFilterForm filterForm) {
        return ApprovedPaymentReportFilter.builder()
                .departmentRegistrationId(filterForm.getDepartmentRegistrationId())
                .fromDate(filterForm.getFromDate())
                .toDate(filterForm.getToDate())
                .financialYear(filterForm.getFinancialYear())
                .searchTerm(filterForm.getSearchTerm())
                .build();
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, 100);
    }

    private List<String> buildFinancialYearOptions() {
        int currentYear = LocalDate.now().getMonthValue() >= 4 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
        return java.util.stream.IntStream.rangeClosed(currentYear - 4, currentYear + 1)
                .mapToObj(startYear -> startYear + "-" + (startYear + 1))
                .toList();
    }

    private String resolveDepartmentName(
            Long departmentRegistrationId,
            List<DepartmentPaymentReportDepartmentOptionView> departments) {
        if (departmentRegistrationId == null) {
            return "All Departments";
        }

        return departments.stream()
                .filter(item -> departmentRegistrationId.equals(item.getDepartmentRegistrationId()))
                .map(DepartmentPaymentReportDepartmentOptionView::getDepartmentName)
                .filter(this::hasText)
                .findFirst()
                .orElse("Department #" + departmentRegistrationId);
    }

    private String buildApprovedDateRangeLabel(AdminApprovedPaymentReportFilterForm filterForm) {
        if (filterForm.getFromDate() == null && filterForm.getToDate() == null) {
            return "All Dates";
        }

        String fromLabel = filterForm.getFromDate() == null ? "From Start" : DATE_FORMATTER.format(filterForm.getFromDate());
        String toLabel = filterForm.getToDate() == null ? "Till Date" : DATE_FORMATTER.format(filterForm.getToDate());
        return fromLabel + " to " + toLabel;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
