package com.maharecruitment.gov.in.auth.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.auth.dto.MenuUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.MstMenu;

public interface MenuManagementService {

    Page<MstMenu> getAll(Pageable pageable);

    List<MstMenu> getParentMenus();

    MstMenu getById(Long menuId);

    MstMenu create(MenuUpsertRequest request);

    MstMenu update(Long menuId, MenuUpsertRequest request);

    void delete(Long menuId);
}
