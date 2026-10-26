package com.maharecruitment.gov.in.web.dto.pip;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;
import com.maharecruitment.gov.in.web.service.pip.EmployeePipService.Decision;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PipDecisionForm {
    @NotNull private Decision decision;
    @NotBlank @Size(max = 2000) private String feedback;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate exitDate;
    @AssertTrue(message = "Confirm the final PIP outcome before saving.") private boolean confirmed;
}
