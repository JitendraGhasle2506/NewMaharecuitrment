package com.maharecruitment.gov.in.web.dto.admin;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuForm {

    @NotBlank(message = "Menu name (English) is required")
    @Size(max = 150, message = "Menu name (English) must not exceed 150 characters")
    private String menuNameEnglish;

    @Size(max = 150, message = "Menu name (Marathi) must not exceed 150 characters")
    private String menuNameMarathi;

    @Size(max = 100, message = "Icon must not exceed 100 characters")
    private String icon;

    @Size(max = 255, message = "URL must not exceed 255 characters")
    private String url;

    @NotNull(message = "Menu type is required")
    private Integer isSubMenu;

    @NotBlank(message = "Status is required")
    private String isActive = "Y";

    private List<Long> roleIds = new ArrayList<>();
}
