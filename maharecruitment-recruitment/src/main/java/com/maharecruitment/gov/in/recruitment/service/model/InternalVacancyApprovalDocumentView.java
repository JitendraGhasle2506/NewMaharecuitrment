package com.maharecruitment.gov.in.recruitment.service.model;

import org.springframework.core.io.Resource;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalVacancyApprovalDocumentView {

    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private Resource resource;
}
