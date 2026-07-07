package com.maharecruitment.gov.in.web.service.hr;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.web.dto.hr.EmployeeImportResult;

public interface EmployeeImportService {

    EmployeeImportResult importCsv(MultipartFile file);

    byte[] buildCsvTemplate();

    String csvTemplateFileName();
}
