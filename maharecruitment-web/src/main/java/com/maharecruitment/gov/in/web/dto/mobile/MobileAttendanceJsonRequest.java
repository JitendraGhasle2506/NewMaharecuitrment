package com.maharecruitment.gov.in.web.dto.mobile;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobileAttendanceJsonRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId,

        @NotNull(message = "Latitude is required.")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required.")
        BigDecimal longitude,

        @Size(max = 1000, message = "Location address must not exceed 1000 characters.")
        String locationAddress,

        @NotBlank(message = "Image data is required.")
        @JsonAlias({ "image", "imageData" })
        String imageBase64,

        @Size(max = 100, message = "Image file name must not exceed 100 characters.")
        @JsonAlias({ "imageName", "fileName" })
        String imageFileName,

        @Size(max = 120, message = "Image content type must not exceed 120 characters.")
        String imageContentType) {
}
