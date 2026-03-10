package com.maharecruitment.gov.in.department.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.maharecruitment.gov.in.department.entity.DepartmentApplicationType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentProjectApplicationForm {

    private Long departmentProjectApplicationId;

    private Long departmentId;

    private Long departmentRegistrationId;

    private String requestId;

    @NotBlank(message = "Project name is required.")
    @Size(max = 200, message = "Project name must not exceed 200 characters.")
    private String projectName;

    @Size(max = 100, message = "Project code must not exceed 100 characters.")
    private String projectCode;

    @NotNull(message = "Application type is required.")
    private DepartmentApplicationType applicationType;

    @Size(max = 1000, message = "Remarks must not exceed 1000 characters.")
    private String remarks;

    @Size(max = 100, message = "MahaIT contact must not exceed 100 characters.")
    private String mahaitContact;

    private MultipartFile workOrderFile;

    private String existingWorkOrderFilePath;

    private String existingWorkOrderOriginalName;

    private BigDecimal totalEstimatedCost;

    @Valid
    private List<DepartmentProjectResourceRequirementForm> resourceRequirements = new ArrayList<>();
}
