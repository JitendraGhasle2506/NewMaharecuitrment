package com.maharecruitment.gov.in.web.dto.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminWeekOffWorkingDayForm {

    private Long id;

    private String viewMonth;

    @NotNull(message = "Working day date is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate workingDate;

    private MultipartFile officeOrderFile;

    private String officeOrderOriginalName;

    private String officeOrderStoredName;

    private String officeOrderPath;

    private String officeOrderContentType;

    private Long officeOrderFileSize;
}
