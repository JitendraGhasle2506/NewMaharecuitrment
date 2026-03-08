package com.maharecruitment.gov.in.auth.service;

import java.util.List;

import com.maharecruitment.gov.in.auth.entity.MstSubMenu;

public interface MstSubMenuService {

    List<MstSubMenu> getAllSubMenus();

    List<MstSubMenu> getSubMenusByMenuIds(List<Long> menuIds);
}
