package com.maharecruitment.gov.in.department.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkOrderDocumentView {

    private String originalFileName;
    private String fullPath;
    private String contentType;
}
