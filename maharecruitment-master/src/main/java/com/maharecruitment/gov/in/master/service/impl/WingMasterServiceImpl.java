package com.maharecruitment.gov.in.master.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.WingMasterDto;
import com.maharecruitment.gov.in.master.entity.WingMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.WingMasterMapper;
import com.maharecruitment.gov.in.master.repository.WingMasterRepository;
import com.maharecruitment.gov.in.master.service.WingMasterService;

@Service
@Transactional(readOnly = true)
public class WingMasterServiceImpl implements WingMasterService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";
    private static final int WING_NAME_MAX_LENGTH = 100;
    private static final Pattern WING_NAME_PATTERN = Pattern.compile("^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/()]+$");

    private final WingMasterRepository repository;
    private final WingMasterMapper mapper;

    public WingMasterServiceImpl(WingMasterRepository repository, WingMasterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public WingMasterDto create(WingMasterDto request) {
        String wingName = normalizeWingName(request.getWingName());
        if (repository.existsByWingNameIgnoreCase(wingName)) {
            throw new DuplicateResourceException("Wing name already exists: " + wingName);
        }

        WingMaster entity = WingMaster.builder()
                .wingName(wingName)
                .activeFlag(normalizeActiveFlag(request.getActiveFlag()))
                .build();
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public WingMasterDto update(Long wingId, WingMasterDto request) {
        WingMaster entity = findWing(wingId);

        String wingName = normalizeWingName(request.getWingName());
        if (repository.existsByWingNameIgnoreCaseAndWingIdNot(wingName, wingId)) {
            throw new DuplicateResourceException("Wing name already exists: " + wingName);
        }

        entity.setWingName(wingName);
        entity.setActiveFlag(normalizeActiveFlag(request.getActiveFlag()));
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public WingMasterDto getById(Long wingId) {
        return mapper.toDto(findWing(wingId));
    }

    @Override
    public List<WingMasterDto> getAll(boolean includeInactive) {
        List<WingMaster> wings = includeInactive
                ? repository.findAllByOrderByWingNameAsc()
                : repository.findByActiveFlagIgnoreCaseOrderByWingNameAsc(ACTIVE);
        return wings.stream().map(mapper::toDto).toList();
    }

    @Override
    public Page<WingMasterDto> search(boolean includeInactive, String searchText, Pageable pageable) {
        String normalizedSearchText = searchText == null ? "" : searchText.trim();
        Page<WingMaster> wings;

        if (normalizedSearchText.isBlank()) {
            wings = includeInactive
                    ? repository.findAll(pageable)
                    : repository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        } else {
            wings = includeInactive
                    ? repository.findByWingNameContainingIgnoreCase(normalizedSearchText, pageable)
                    : repository.findByActiveFlagIgnoreCaseAndWingNameContainingIgnoreCase(
                            ACTIVE,
                            normalizedSearchText,
                            pageable);
        }

        return wings.map(mapper::toDto);
    }

    @Override
    @Transactional
    public void softDelete(Long wingId) {
        updateStatus(wingId, INACTIVE);
    }

    @Override
    @Transactional
    public void restore(Long wingId) {
        updateStatus(wingId, ACTIVE);
    }

    @Override
    @Transactional
    public void toggleStatus(Long wingId) {
        WingMaster entity = findWing(wingId);
        entity.setActiveFlag(ACTIVE.equalsIgnoreCase(entity.getActiveFlag()) ? INACTIVE : ACTIVE);
        repository.save(entity);
    }

    private void updateStatus(Long wingId, String activeFlag) {
        WingMaster entity = findWing(wingId);
        entity.setActiveFlag(activeFlag);
        repository.save(entity);
    }

    private WingMaster findWing(Long wingId) {
        return repository.findByWingId(wingId)
                .orElseThrow(() -> new ResourceNotFoundException("Wing not found with id: " + wingId));
    }

    private String normalizeWingName(String wingName) {
        String normalized = wingName == null ? "" : wingName.trim();
        if (normalized.isBlank()) {
            throw new BusinessValidationException("Wing name is required");
        }
        if (normalized.length() > WING_NAME_MAX_LENGTH) {
            throw new BusinessValidationException("Wing name must not exceed 100 characters");
        }
        if (!WING_NAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessValidationException(
                    "Wing name can contain alphabets, numbers, spaces, hyphen, slash and brackets only");
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
