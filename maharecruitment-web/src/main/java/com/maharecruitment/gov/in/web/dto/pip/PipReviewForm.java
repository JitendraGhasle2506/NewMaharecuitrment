package com.maharecruitment.gov.in.web.dto.pip;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PipReviewForm {
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate reviewDate;
    @NotNull @Min(1) @Max(5) private Integer rating;
    @NotBlank @Size(max = 2000) private String feedback;
}
