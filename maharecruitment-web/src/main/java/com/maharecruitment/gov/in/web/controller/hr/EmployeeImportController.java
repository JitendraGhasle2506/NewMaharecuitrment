package com.maharecruitment.gov.in.web.controller.hr;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.hr.EmployeeImportResult;
import com.maharecruitment.gov.in.web.service.hr.EmployeeImportService;

@Controller
@RequestMapping("/hr/employees/import")
@PreAuthorize("hasAuthority('ROLE_HR')")
public class EmployeeImportController {

    private final EmployeeImportService employeeImportService;

    public EmployeeImportController(EmployeeImportService employeeImportService) {
        this.employeeImportService = employeeImportService;
    }

    @GetMapping
    public String view() {
        return "hr/employee-import";
    }

    @PostMapping
    public String importEmployees(
            @RequestParam("file") MultipartFile file,
            Model model) {
        try {
            EmployeeImportResult result = employeeImportService.importCsv(file);
            model.addAttribute("importResult", result);
            if (result.failureCount() > 0) {
                model.addAttribute(
                        "warningMessage",
                        "Import completed with errors. Please review failed rows below.");
            } else {
                model.addAttribute("successMessage", "Employee import completed successfully.");
            }
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
        }
        return "hr/employee-import";
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] csv = employeeImportService.buildCsvTemplate();
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(employeeImportService.csvTemplateFileName())
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
