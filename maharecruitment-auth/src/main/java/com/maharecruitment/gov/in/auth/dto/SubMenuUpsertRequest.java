package com.maharecruitment.gov.in.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubMenuUpsertRequest {

    private Long menuId;
    private String subMenuNameEnglish;
    private String subMenuNameMarathi;
    private String controllerName;
    private String url;
    private String icon;
    private Character isActive;
}
