package com.maharecruitment.gov.in.master.dto;

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import com.maharecruitment.gov.in.master.entity.DesignationType;

@Getter
@Setter
public class ManpowerDesignationMasterRequest {

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Designation name is required")
    private String designationName;

    @NotBlank(message = "Role name is required")
    private String roleName;

    @NotNull(message = "Designation type is required")
    private DesignationType designationType = DesignationType.O;
    
    @NotBlank(message = "Education Qualification is required")
    private String educationalQualification;
    private String certification;
    private String activeFlag;
    private Set<Long> levelIds = new HashSet<>();
}

