package com.maharecruitment.gov.in.master.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.CellMasterDto;

public interface CellMasterService {

    CellMasterDto create(CellMasterDto request);

    CellMasterDto update(Long cellId, CellMasterDto request);

    CellMasterDto getById(Long cellId);

    List<CellMasterDto> getAll(boolean includeInactive);

    Page<CellMasterDto> search(boolean includeInactive, String searchText, Pageable pageable);

    void activate(Long cellId);

    void deactivate(Long cellId);

    void toggleStatus(Long cellId);
}
