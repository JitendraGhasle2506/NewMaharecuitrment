package com.maharecruitment.gov.in.master.dto;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManpowerDesignationMasterRequest {

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Designation name is required")
    private String designationName;

    private String roleName;
    private String educationalQualification;
    private String certification;
    private String activeFlag;
    private Set<Long> levelIds = new HashSet<>();
}

