package com.maharecruitment.gov.in.master.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class LocationMasterDto {

    private Long locationId;

    @NotBlank(message = "Address is required")
    @Size(max = 150, message = "Address must not exceed 150 characters")
    @Pattern(
            regexp = "^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/().,]+$",
            message = "Address can contain alphabets, numbers, spaces, hyphen, slash, brackets, dot and comma only")
    private String locationName;

    @Size(max = 150, message = "Office name must not exceed 150 characters")
    @Pattern(
            regexp = "^$|^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/().,&']+$",
            message = "Office name can contain alphabets, numbers, spaces, hyphen, slash, brackets, dot, comma, ampersand and apostrophe only")
    private String officeName;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90")
    @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90")
    @Digits(integer = 3, fraction = 7, message = "Latitude can have up to 7 decimal places")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180")
    @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180")
    @Digits(integer = 3, fraction = 7, message = "Longitude can have up to 7 decimal places")
    private BigDecimal longitude;

    private String activeFlag;
    private Long createdUserId;
    private Long updatedUserId;
    private LocalDateTime createdDateTime;
    private LocalDateTime updatedDateTime;
}
