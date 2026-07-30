package com.maharecruitment.gov.in.auth.dto;

import java.util.ArrayList;
import java.util.List;

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
    private List<Long> roleIds = new ArrayList<>();
}
