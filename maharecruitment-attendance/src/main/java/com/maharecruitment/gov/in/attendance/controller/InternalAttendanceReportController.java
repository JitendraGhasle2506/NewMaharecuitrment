package com.maharecruitment.gov.in.attendance.controller;

import java.text.DateFormatSymbols;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.attendance.service.InternalAttendanceReportPdfGenerator;
import com.maharecruitment.gov.in.attendance.service.InternalAttendanceReportTimeCsvGenerator;
import com.maharecruitment.gov.in.attendance.service.InternalEmployeeAttendanceReportService;
import com.maharecruitment.gov.in.attendance.service.model.GeneratedAttendanceReportDocument;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportFilter;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportRow;
import com.maharecruitment.gov.in.attendance.service.model.InternalAttendanceReportView;
import com.maharecruitment.gov.in.master.entity.AgencyStatus;
import com.maharecruitment.gov.in.master.repository.AgencyMasterRepository;

@Controller
@RequestMapping("/hr/internal-attendance-report")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class InternalAttendanceReportController {

    private static final int SCREEN_PAGE_SIZE = 20;

    private final InternalEmployeeAttendanceReportService internalAttendanceReportService;
    private final InternalAttendanceReportPdfGenerator internalAttendanceReportPdfGenerator;
    private final InternalAttendanceReportTimeCsvGenerator internalAttendanceReportTimeCsvGenerator;
    private final AgencyMasterRepository agencyMasterRepository;

    public InternalAttendanceReportController(
            InternalEmployeeAttendanceReportService internalAttendanceReportService,
            InternalAttendanceReportPdfGenerator internalAttendanceReportPdfGenerator,
            InternalAttendanceReportTimeCsvGenerator internalAttendanceReportTimeCsvGenerator,
            AgencyMasterRepository agencyMasterRepository) {
        this.internalAttendanceReportService = internalAttendanceReportService;
        this.internalAttendanceReportPdfGenerator = internalAttendanceReportPdfGenerator;
        this.internalAttendanceReportTimeCsvGenerator = internalAttendanceReportTimeCsvGenerator;
        this.agencyMasterRepository = agencyMasterRepository;
    }

    @GetMapping
    public String internalAttendanceReport(
            @ModelAttribute("filter") InternalAttendanceReportFilter filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model) {
        InternalAttendanceReportView report = internalAttendanceReportService.buildReport(filter);
        Page<InternalAttendanceReportRow> reportPage = paginateRows(report, page);
        model.addAttribute("filter", report.getFilter());
        model.addAttribute("report", report);
        model.addAttribute("reportPage", reportPage);
        model.addAttribute("agencyOptions", getAgencyOptions());
        model.addAttribute("monthNames", getMonthNames());
        model.addAttribute("yearOptions", buildYearOptions(report.getFilter().getYear()));
        return "attendance/internal-attendance-report";
    }

    @GetMapping("/download/pdf")
    public ResponseEntity<byte[]> downloadPdfReport(@ModelAttribute InternalAttendanceReportFilter filter) {
        InternalAttendanceReportView report = internalAttendanceReportService.buildReport(filter);
        GeneratedAttendanceReportDocument document = internalAttendanceReportPdfGenerator.generate(report);
        return buildDownloadResponse(document);
    }

    @GetMapping("/download/time-csv")
    public ResponseEntity<byte[]> downloadAttendanceTimeReport(@ModelAttribute InternalAttendanceReportFilter filter) {
        InternalAttendanceReportView report = internalAttendanceReportService.buildReport(filter);
        GeneratedAttendanceReportDocument document = internalAttendanceReportTimeCsvGenerator.generate(report);
        return buildDownloadResponse(document);
    }

    private ResponseEntity<byte[]> buildDownloadResponse(GeneratedAttendanceReportDocument document) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(document.originalFileName(), StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.parseMediaType(document.contentType()));
        headers.setContentLength(document.size());

        return ResponseEntity.ok()
                .headers(headers)
                .body(document.bytes());
    }

    private Map<Integer, String> getMonthNames() {
        Map<Integer, String> monthMap = new TreeMap<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            monthMap.put(i + 1, months[i]);
        }
        return monthMap;
    }

    private Map<Long, String> getAgencyOptions() {
        return agencyMasterRepository.findByStatusOrderByAgencyNameAsc(AgencyStatus.ACTIVE)
                .stream()
                .collect(LinkedHashMap::new,
                        (map, agency) -> map.put(agency.getAgencyId(), agency.getAgencyName()),
                        LinkedHashMap::putAll);
    }

    private Page<InternalAttendanceReportRow> paginateRows(
            InternalAttendanceReportView report,
            int requestedPage) {
        List<InternalAttendanceReportRow> allRows =
                report.getRows() == null ? List.of() : report.getRows();

        if (allRows.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(0, SCREEN_PAGE_SIZE), 0);
        }

        int totalElements = allRows.size();
        int totalPages = (int) Math.ceil((double) totalElements / SCREEN_PAGE_SIZE);
        int pageNumber = Math.min(Math.max(requestedPage, 0), totalPages - 1);
        int fromIndex = pageNumber * SCREEN_PAGE_SIZE;
        int toIndex = Math.min(fromIndex + SCREEN_PAGE_SIZE, totalElements);

        return new PageImpl<>(
                allRows.subList(fromIndex, toIndex),
                PageRequest.of(pageNumber, SCREEN_PAGE_SIZE),
                totalElements);
    }

    private Map<Integer, Integer> buildYearOptions(Integer selectedYear) {
        int currentYear = LocalDate.now().getYear();
        int centerYear = selectedYear != null ? selectedYear : currentYear;
        Map<Integer, Integer> years = new LinkedHashMap<>();
        for (int year = centerYear - 3; year <= centerYear + 2; year++) {
            years.put(year, year);
        }
        if (!years.containsKey(currentYear)) {
            years.put(currentYear, currentYear);
        }
        return years.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);
    }
}
