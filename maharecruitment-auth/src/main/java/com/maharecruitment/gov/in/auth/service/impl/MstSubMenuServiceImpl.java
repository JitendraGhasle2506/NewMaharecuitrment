package com.maharecruitment.gov.in.auth.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.auth.entity.MstSubMenu;
import com.maharecruitment.gov.in.auth.repository.MstSubMenuRepository;
import com.maharecruitment.gov.in.auth.service.MstSubMenuService;

@Service
public class MstSubMenuServiceImpl implements MstSubMenuService {

    private final MstSubMenuRepository mstSubMenuRepository;

    public MstSubMenuServiceImpl(MstSubMenuRepository mstSubMenuRepository) {
        this.mstSubMenuRepository = mstSubMenuRepository;
    }

    @Override
    public List<MstSubMenu> getAllSubMenus() {
        return mstSubMenuRepository.findAllByOrderByMenuMenuIdAscSubMenuIdAsc();
    }

    @Override
    public List<MstSubMenu> getSubMenusByMenuIds(List<Long> menuIds) {
        if (menuIds == null || menuIds.isEmpty()) {
            return List.of();
        }
        return mstSubMenuRepository.findByMenuMenuIdInOrderByMenuMenuIdAscSubMenuIdAsc(menuIds);
    }
}
