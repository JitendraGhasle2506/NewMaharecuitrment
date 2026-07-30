package com.maharecruitment.gov.in.master.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.maharecruitment.gov.in.master.dto.LocationMasterDto;

public interface LocationMasterService {

    LocationMasterDto create(LocationMasterDto request);

    LocationMasterDto update(Long locationId, LocationMasterDto request);

    LocationMasterDto getById(Long locationId);

    List<LocationMasterDto> getAll(boolean includeInactive);

    Page<LocationMasterDto> search(boolean includeInactive, String searchText, Pageable pageable);

    void deactivate(Long locationId);

    void activate(Long locationId);

    void toggleStatus(Long locationId);
}
