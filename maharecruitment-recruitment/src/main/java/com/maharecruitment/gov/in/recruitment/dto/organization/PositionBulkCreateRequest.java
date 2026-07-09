package com.maharecruitment.gov.in.recruitment.dto.organization;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionBulkCreateRequest {

    private Long cellId;

    @Size(max = 100, message = "Cell name must not exceed 100 characters")
    private String cellName;

    private Long designationId;

    @Size(max = 150, message = "Designation name must not exceed 150 characters")
    private String designationName;

    @Size(max = 10, message = "Level code must not exceed 10 characters")
    private String levelCode;

    @Min(value = 1, message = "Number of positions must be at least 1")
    @Max(value = 500, message = "Number of positions cannot exceed 500")
    private Integer positionCount;
}
