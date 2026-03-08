package com.maharecruitment.gov.in.auth.service;

import java.util.List;

import com.maharecruitment.gov.in.auth.entity.MstMenu;

public interface MstMenuService {

    List<MstMenu> findMenusByRoleIds(List<Long> roleIds);
}
