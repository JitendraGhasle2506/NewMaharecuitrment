package com.maharecruitment.gov.in.master.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignationCategoryMasterRequest {

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String activeFlag;
}
