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
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.CellMasterMapper;
import com.maharecruitment.gov.in.master.repository.CellMasterRepository;
import com.maharecruitment.gov.in.master.service.CellMasterService;

@Service
@Transactional(readOnly = true)
public class CellMasterServiceImpl implements CellMasterService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";
    private static final int CELL_NAME_MAX_LENGTH = 100;
    private static final Pattern CELL_NAME_PATTERN = Pattern.compile("^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/()]+$");

    private final CellMasterRepository repository;
    private final CellMasterMapper mapper;

    public CellMasterServiceImpl(CellMasterRepository repository, CellMasterMapper mapper) {
        this.repository = repository;
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
                : repository.findByActiveFlagIgnoreCaseOrderByCellNameAsc(ACTIVE);
        return cells.stream().map(mapper::toDto).toList();
    }

    @Override
    public Page<CellMasterDto> search(boolean includeInactive, String searchText, Pageable pageable) {
        String normalizedSearchText = searchText == null ? "" : searchText.trim();
        Page<CellMaster> cells;

        if (normalizedSearchText.isBlank()) {
            cells = includeInactive
                    ? repository.findAll(pageable)
                    : repository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        } else {
            cells = includeInactive
                    ? repository.findByCellNameContainingIgnoreCase(normalizedSearchText, pageable)
                    : repository.findByActiveFlagIgnoreCaseAndCellNameContainingIgnoreCase(
                            ACTIVE,
                            normalizedSearchText,
                            pageable);
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
