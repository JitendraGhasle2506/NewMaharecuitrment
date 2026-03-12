package com.maharecruitment.gov.in.web.dto.agency;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgencyCandidateRowForm {

    private String candidateName;

    private String email;

    private String mobile;

    private String candidateEducation;

    private BigDecimal totalExp;

    private BigDecimal relevantExp;

    private String joiningTime;

    private MultipartFile resumeFile;
}
