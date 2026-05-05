package com.maharecruitment.gov.in.web.dto.agency;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyCandidateRowForm {

    @NotBlank(message = "Candidate name is required")
    private String candidateName;

    @NotBlank(message = "Candidate email is required")
    @Email(message = "Candidate email must be valid")
    private String email;

    @NotBlank(message = "Candidate mobile is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Candidate mobile must be 10 digits")
    private String mobile;

    @NotBlank(message = "Candidate qualification is required")
    private String candidateEducation;

    @NotNull(message = "Total experience is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total experience cannot be negative")
    private BigDecimal totalExp;

    @NotNull(message = "Relevant experience is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Relevant experience cannot be negative")
    private BigDecimal relevantExp;

    @NotBlank(message = "Joining time is required")
    private String joiningTime;

    private MultipartFile resumeFile;
}
