package com.maharecruitment.gov.in.auth.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.auth.entity.MstMenu;
import com.maharecruitment.gov.in.auth.repository.MstMenuRepository;
import com.maharecruitment.gov.in.auth.service.MstMenuService;

@Service
public class MstMenuServiceImpl implements MstMenuService {

    private final MstMenuRepository mstMenuRepository;

    public MstMenuServiceImpl(MstMenuRepository mstMenuRepository) {
        this.mstMenuRepository = mstMenuRepository;
    }

    @Override
    public List<MstMenu> findMenusByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return mstMenuRepository.findMenusByRoleIds(roleIds);
    }
}
