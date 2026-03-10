package com.maharecruitment.gov.in.auth.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.auth.dto.SubMenuUpsertRequest;
import com.maharecruitment.gov.in.auth.entity.MstSubMenu;

public interface SubMenuManagementService {

    Page<MstSubMenu> getAll(Pageable pageable);

    MstSubMenu getById(Long subMenuId);

    MstSubMenu create(SubMenuUpsertRequest request);

    MstSubMenu update(Long subMenuId, SubMenuUpsertRequest request);

    void delete(Long subMenuId);
}
