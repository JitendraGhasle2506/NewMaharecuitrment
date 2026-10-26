package com.maharecruitment.gov.in.web.dto.pip;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PipStartForm {
    @NotNull @Positive private Long employeeId;
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate startDate;
    @Min(1) @Max(2) private int months = 1;
    @NotBlank @Size(max = 2000) private String objectives;
    @NotBlank(message = "Enter the reason for placing the employee on PIP.")
    @Size(max = 2000, message = "Reason for PIP must be at most 2,000 characters.")
    private String reason;
}
