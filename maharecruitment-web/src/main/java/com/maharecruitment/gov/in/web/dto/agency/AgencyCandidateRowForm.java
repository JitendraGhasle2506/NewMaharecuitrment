package com.maharecruitment.gov.in.web.dto.agency;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyCandidateRowForm {

    @NotBlank(message = "Candidate name is required")
    @Size(min = 2, max = 100, message = "Candidate name must be between 2 and 100 characters")
    @Pattern(regexp = "^[^\\s].*$", message = "Candidate name must not start with a space")
    @Pattern(regexp = "^[^0-9]*$", message = "Candidate name must not contain numbers")
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

    @NotNull(message = "Current CTC is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Current CTC cannot be negative")
    @Digits(integer = 12, fraction = 2, message = "Current CTC must contain at most 12 digits and 2 decimals")
    private BigDecimal currentCtc;

    @NotNull(message = "Please specify whether the candidate has resigned")
    private Boolean resigned;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastWorkingDay;

    @NotBlank(message = "Joining time is required")
    private String joiningTime;

    private MultipartFile resumeFile;
}
