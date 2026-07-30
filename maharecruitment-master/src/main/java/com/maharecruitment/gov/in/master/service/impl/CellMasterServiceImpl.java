package com.maharecruitment.gov.in.master.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.CellMasterDto;
import com.maharecruitment.gov.in.master.entity.CellMaster;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.CellMasterMapper;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.repository.WingMasterRepository;
import com.maharecruitment.gov.in.master.service.CellMasterService;

@Service
@Transactional(readOnly = true)
public class CellMasterServiceImpl implements CellMasterService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";
    private static final int CELL_NAME_MAX_LENGTH = 100;
    private static final Pattern CELL_NAME_PATTERN = Pattern.compile("^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/()]+$");

    private final CellMasterRepository repository;
    private final WingMasterRepository wingRepository;
    private final CellMasterMapper mapper;

    public CellMasterServiceImpl(
            CellMasterRepository repository,
            WingMasterRepository wingRepository,
            CellMasterMapper mapper) {
        this.repository = repository;
        this.wingRepository = wingRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public CellMasterDto create(CellMasterDto request) {
        String cellName = normalizeCellName(request.getCellName());
        if (repository.existsByCellNameIgnoreCase(cellName)) {
            throw new DuplicateResourceException("Cell name already exists: " + cellName);
        }

        CellMaster entity = CellMaster.builder()
                .cellName(cellName)
                .wing(resolveActiveWing(request.getWingId()))
                .activeFlag(normalizeActiveFlag(request.getActiveFlag()))
                .build();
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public CellMasterDto update(Long cellId, CellMasterDto request) {
        CellMaster entity = repository.findByCellId(cellId)
                .orElseThrow(() -> new ResourceNotFoundException("Cell not found with id: " + cellId));

        String cellName = normalizeCellName(request.getCellName());
        if (repository.existsByCellNameIgnoreCaseAndCellIdNot(cellName, cellId)) {
            throw new DuplicateResourceException("Cell name already exists: " + cellName);
        }

        entity.setCellName(cellName);
        entity.setWing(resolveActiveWing(request.getWingId()));
        entity.setActiveFlag(normalizeActiveFlag(request.getActiveFlag()));
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public CellMasterDto getById(Long cellId) {
        return repository.findByCellId(cellId)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Cell not found with id: " + cellId));
    }

    @Override
    public List<CellMasterDto> getAll(boolean includeInactive) {
        List<CellMaster> cells = includeInactive
                ? repository.findAllByOrderByCellNameAsc()
                : repository.findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE, ACTIVE);
        return cells.stream().map(mapper::toDto).toList();
    }

    @Override
    public Page<CellMasterDto> search(Long wingId, boolean includeInactive, String searchText, Pageable pageable) {
        String normalizedSearchText = searchText == null ? "" : searchText.trim();
        Page<CellMaster> cells;

        if (normalizedSearchText.isBlank()) {
            cells = findByWingAndStatus(wingId, includeInactive, pageable);
        } else {
            cells = findByWingStatusAndSearchText(wingId, includeInactive, normalizedSearchText, pageable);
        }

        return cells.map(mapper::toDto);
    }

    @Override
    @Transactional
    public void activate(Long cellId) {
        updateStatus(cellId, ACTIVE);
    }

    @Override
    @Transactional
    public void deactivate(Long cellId) {
        updateStatus(cellId, INACTIVE);
    }

    @Override
    @Transactional
    public void toggleStatus(Long cellId) {
        CellMaster entity = repository.findByCellId(cellId)
                .orElseThrow(() -> new ResourceNotFoundException("Cell not found with id: " + cellId));
        entity.setActiveFlag(ACTIVE.equalsIgnoreCase(entity.getActiveFlag()) ? INACTIVE : ACTIVE);
        repository.save(entity);
    }

    private void updateStatus(Long cellId, String activeFlag) {
        CellMaster entity = repository.findByCellId(cellId)
                .orElseThrow(() -> new ResourceNotFoundException("Cell not found with id: " + cellId));
        entity.setActiveFlag(activeFlag);
        repository.save(entity);
    }

    private Page<CellMaster> findByWingAndStatus(Long wingId, boolean includeInactive, Pageable pageable) {
        if (wingId == null) {
            return includeInactive
                    ? repository.findAll(pageable)
                    : repository.findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCase(ACTIVE, ACTIVE, pageable);
        }
        return includeInactive
                ? repository.findByWing_WingId(wingId, pageable)
                : repository.findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCase(
                        wingId,
                        ACTIVE,
                        ACTIVE,
                        pageable);
    }

    private Page<CellMaster> findByWingStatusAndSearchText(
            Long wingId,
            boolean includeInactive,
            String searchText,
            Pageable pageable) {
        if (wingId == null) {
            return includeInactive
                    ? repository.findByCellNameContainingIgnoreCase(searchText, pageable)
                    : repository.findByActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseAndCellNameContainingIgnoreCase(
                            ACTIVE,
                            ACTIVE,
                            searchText,
                            pageable);
        }
        return includeInactive
                ? repository.findByWing_WingIdAndCellNameContainingIgnoreCase(wingId, searchText, pageable)
                : repository
                        .findByWing_WingIdAndActiveFlagIgnoreCaseAndWing_ActiveFlagIgnoreCaseAndCellNameContainingIgnoreCase(
                                wingId,
                                ACTIVE,
                                ACTIVE,
                                searchText,
                                pageable);
    }

    private WingMaster resolveActiveWing(Long wingId) {
        if (wingId == null) {
            throw new BusinessValidationException("Wing is required.");
        }
        WingMaster wing = wingRepository.findByWingId(wingId)
                .orElseThrow(() -> new ResourceNotFoundException("Wing not found with id: " + wingId));
        if (!ACTIVE.equalsIgnoreCase(wing.getActiveFlag())) {
            throw new BusinessValidationException("Selected wing is inactive.");
        }
        return wing;
    }

    private String normalizeCellName(String cellName) {
        String normalized = cellName == null ? "" : cellName.trim();
        if (normalized.isBlank()) {
            throw new BusinessValidationException("Cell name is required");
        }
        if (normalized.length() > CELL_NAME_MAX_LENGTH) {
            throw new BusinessValidationException("Cell name must not exceed 100 characters");
        }
        if (!CELL_NAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessValidationException(
                    "Cell name can contain alphabets, numbers, spaces, hyphen, slash and brackets only");
        }
        return normalized;
    }

    private String normalizeActiveFlag(String activeFlag) {
        if (activeFlag == null || activeFlag.isBlank()) {
            return ACTIVE;
        }
        return INACTIVE.equals(activeFlag.trim().toUpperCase(Locale.ROOT)) ? INACTIVE : ACTIVE;
    }
}
