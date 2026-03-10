package com.maharecruitment.gov.in.department.service.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoredDocument {

    private String originalFileName;
    private String fullPath;
    private String contentType;
    private long fileSize;
}
