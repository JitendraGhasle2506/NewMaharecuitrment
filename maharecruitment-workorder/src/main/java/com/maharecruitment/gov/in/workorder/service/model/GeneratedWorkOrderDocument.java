package com.maharecruitment.gov.in.workorder.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneratedWorkOrderDocument {

    private String originalFileName;
    private String contentType;
    private byte[] bytes;
    private Long size;

    public String originalFileName() {
        return originalFileName;
    }

    public String contentType() {
        return contentType;
    }

    public byte[] bytes() {
        return bytes;
    }

    public Long size() {
        return size;
    }
}
