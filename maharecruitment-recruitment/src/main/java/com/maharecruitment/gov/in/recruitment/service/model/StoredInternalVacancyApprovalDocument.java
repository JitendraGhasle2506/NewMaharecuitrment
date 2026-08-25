package com.maharecruitment.gov.in.recruitment.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoredInternalVacancyApprovalDocument {

    private String originalFileName;
    private String fullPath;
    private String contentType;
    private Long fileSize;
}
