package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationCategoryMasterResponse {

    private Long categoryId;
    private String categoryName;
    private String activeFlag;
    private Long createdUserId;
    private Long updatedUserId;
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;
}
