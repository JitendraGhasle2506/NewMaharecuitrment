package com.maharecruitment.gov.in.web.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubMenuForm {

    @NotNull(message = "Parent menu is required")
    private Long menuId;

    @NotBlank(message = "Submenu name (English) is required")
    @Size(max = 150, message = "Submenu name (English) must not exceed 150 characters")
    private String subMenuNameEnglish;

    @Size(max = 150, message = "Submenu name (Marathi) must not exceed 150 characters")
    private String subMenuNameMarathi;

    @Size(max = 150, message = "Controller name must not exceed 150 characters")
    private String controllerName;

    @NotBlank(message = "URL is required")
    @Size(max = 255, message = "URL must not exceed 255 characters")
    private String url;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    @NotBlank(message = "Status is required")
    private String isActive = "Y";
}
