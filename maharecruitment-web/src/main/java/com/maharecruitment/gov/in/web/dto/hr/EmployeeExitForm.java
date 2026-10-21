package com.maharecruitment.gov.in.web.dto.hr;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeExitForm {
    @NotNull(message = "Enter the last working date.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastWorkingDate;

    @NotBlank(message = "Enter a reason for the exit.")
    @Size(max = 1000, message = "The reason must be at most 1,000 characters.")
    private String reason;

    @AssertTrue(message = "Confirm that the employee has left and access should be disabled.")
    private boolean confirmed;
}
