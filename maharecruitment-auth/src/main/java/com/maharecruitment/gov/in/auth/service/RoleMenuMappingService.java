package com.maharecruitment.gov.in.auth.service;

import java.util.List;
import java.util.Map;

import com.maharecruitment.gov.in.auth.entity.MstMenu;

public interface RoleMenuMappingService {

    List<MstMenu> getAllMenus();

    List<MstMenu> getMenusByRoleId(Long roleId);

    List<Long> getMenuIdsByRoleId(Long roleId);

    Map<Long, Long> countMenusByRoleIds(List<Long> roleIds);

    void replaceRoleMenuMappings(Long roleId, List<Long> menuIds);

    void clearRoleMenuMappings(Long roleId);
}
