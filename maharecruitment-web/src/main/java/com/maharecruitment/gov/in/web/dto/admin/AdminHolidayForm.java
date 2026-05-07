package com.maharecruitment.gov.in.web.dto.admin;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminHolidayForm {

    private Long id;

    private String viewMonth;

    @NotNull(message = "Holiday date is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;

    @NotBlank(message = "Holiday name is required.")
    @Size(max = 150, message = "Holiday name must be 150 characters or less.")
    private String holidayName;
}
