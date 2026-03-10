package com.maharecruitment.gov.in.auth.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuUpsertRequest {

    private String menuNameEnglish;
    private String menuNameMarathi;
    private String icon;
    private String url;
    private Integer isSubMenu;
    private String isActive;
    private List<Long> roleIds = new ArrayList<>();
}
