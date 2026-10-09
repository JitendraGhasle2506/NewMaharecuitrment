package com.maharecruitment.gov.in.attendance.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.attendance.entity.CalendarEntryType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommonCalendarEntryForm {

    private Long id;
    private String source;
    private String viewMonth;
    private CalendarDateMode dateMode = CalendarDateMode.SINGLE;

    @NotNull(message = "Entry type is required.")
    private CalendarEntryType entryType = CalendarEntryType.HOLIDAY;

    @NotNull(message = "Date is required.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate entryDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotBlank(message = "Title is required.")
    @Size(max = 150, message = "Title must be 150 characters or less.")
    private String title;

    @Size(max = 500, message = "Description must be 500 characters or less.")
    private String description;

    private List<MultipartFile> photos = new ArrayList<>();
}
