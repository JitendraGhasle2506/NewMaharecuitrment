package com.maharecruitment.gov.in.master.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.WingMasterDto;

public interface WingMasterService {

    WingMasterDto create(WingMasterDto request);

    WingMasterDto update(Long wingId, WingMasterDto request);

    WingMasterDto getById(Long wingId);

    List<WingMasterDto> getAll(boolean includeInactive);

    Page<WingMasterDto> search(boolean includeInactive, String searchText, Pageable pageable);

    void softDelete(Long wingId);

    void restore(Long wingId);

    void toggleStatus(Long wingId);
}
