package com.maharecruitment.gov.in.master.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WingMasterDto {

    private Long wingId;

    @NotBlank(message = "Wing name is required")
    @Size(max = 100, message = "Wing name must not exceed 100 characters")
    @Pattern(
            regexp = "^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/()]+$",
            message = "Wing name can contain alphabets, numbers, spaces, hyphen, slash and brackets only")
    private String wingName;

    private String activeFlag;
    private Long createdUserId;
    private Long updatedUserId;
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;
}
