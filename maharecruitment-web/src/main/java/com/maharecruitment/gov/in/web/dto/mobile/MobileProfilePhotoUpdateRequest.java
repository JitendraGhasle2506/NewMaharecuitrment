package com.maharecruitment.gov.in.web.dto.mobile;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobileProfilePhotoUpdateRequest(
        @NotNull(message = "Employee ID is required.")
        Long employeeId,

        @NotBlank(message = "Photo data is required.")
        @JsonAlias({ "photoBase64", "photoData", "image", "imageBase64" })
        String photo,

        @Size(max = 100, message = "Photo file name must not exceed 100 characters.")
        @JsonAlias({ "photoName", "imageName", "fileName" })
        String photoFileName,

        @Size(max = 120, message = "Photo content type must not exceed 120 characters.")
        @JsonAlias({ "imageContentType", "contentType" })
        String photoContentType,

        @Size(max = 200000, message = "Embedding must not exceed 200000 characters.")
        String embedding) {
}
