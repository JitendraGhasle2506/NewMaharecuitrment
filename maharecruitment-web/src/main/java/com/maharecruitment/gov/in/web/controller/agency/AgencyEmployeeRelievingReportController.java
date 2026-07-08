package com.maharecruitment.gov.in.web.controller.agency;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.agency.AgencyUserContext;
import com.maharecruitment.gov.in.web.service.hr.EmployeeRelievingService;

import java.security.Principal;

@Controller
@RequestMapping("/agency/reports/relieving")
@PreAuthorize("hasAuthority('ROLE_AGENCY')")
public class AgencyEmployeeRelievingReportController {

    private final EmployeeRelievingService relievingService;
    private final AgencyAccessService agencyAccessService;
    private final com.maharecruitment.gov.in.web.service.hr.impl.AgencyEmployeeRelievingReportPdfGenerator pdfGenerator;

    public AgencyEmployeeRelievingReportController(EmployeeRelievingService relievingService,
                                                   AgencyAccessService agencyAccessService,
                                                   com.maharecruitment.gov.in.web.service.hr.impl.AgencyEmployeeRelievingReportPdfGenerator pdfGenerator) {
        this.relievingService = relievingService;
        this.agencyAccessService = agencyAccessService;
        this.pdfGenerator = pdfGenerator;
    }

    @GetMapping
    public String viewReport(Principal principal, Model model,
                             @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
                             @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        AgencyUserContext context = agencyAccessService.requireActiveAgencyContext(principal.getName());
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto> recordPage = relievingService.getRelievingRecordsByAgency(context.agencyId(), pageable);
        
        model.addAttribute("recordPage", recordPage);
        return "agency/relieving-report";
    }

    @GetMapping("/export/excel")
    public void exportExcel(Principal principal, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        AgencyUserContext context = agencyAccessService.requireActiveAgencyContext(principal.getName());
        java.util.List<com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto> records = relievingService.getRelievingRecordsByAgency(context.agencyId());

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=relieving_report.xlsx");

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Relieving Report");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Employee Name", "Resignation Date", "Exit Date", "Status"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto record : records) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(record.getEmployeeCode() != null ? record.getEmployeeCode() : "N/A");
                row.createCell(1).setCellValue(record.getEmployeeName() != null ? record.getEmployeeName() : "");
                row.createCell(2).setCellValue(record.getResignDate() != null ? record.getResignDate().toString() : "-");
                row.createCell(3).setCellValue(record.getExitDate() != null ? record.getExitDate().toString() : "-");
                row.createCell(4).setCellValue(record.getStatus() != null ? record.getStatus() : "");
            }
            workbook.write(response.getOutputStream());
        }
    }

    @GetMapping("/export/pdf")
    public void exportPdf(Principal principal, jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        AgencyUserContext context = agencyAccessService.requireActiveAgencyContext(principal.getName());
        java.util.List<com.maharecruitment.gov.in.recruitment.dto.employee.EmployeeRelievingDto> records = relievingService.getRelievingRecordsByAgency(context.agencyId());

        byte[] pdfBytes = pdfGenerator.generate(records, context.agencyName());

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=relieving_report.pdf");
        response.setContentLength(pdfBytes.length);
        
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }
}
