package com.maharecruitment.gov.in.master.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.master.dto.LocationMasterDto;
import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.master.exception.BusinessValidationException;
import com.maharecruitment.gov.in.master.exception.DuplicateResourceException;
import com.maharecruitment.gov.in.master.exception.ResourceNotFoundException;
import com.maharecruitment.gov.in.master.mapper.LocationMasterMapper;
import com.maharecruitment.gov.in.master.repository.LocationMasterRepository;
import com.maharecruitment.gov.in.master.service.LocationMasterService;

@Service
@Transactional(readOnly = true)
public class LocationMasterServiceImpl implements LocationMasterService {

    private static final String ACTIVE = "Y";
    private static final String INACTIVE = "N";
    private static final int LOCATION_NAME_MAX_LENGTH = 150;
    private static final int DEPARTMENT_NAME_MAX_LENGTH = 100;
    private static final Pattern LOCATION_NAME_PATTERN = Pattern.compile("^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/().,]+$");
    private static final Pattern OFFICE_NAME_PATTERN = Pattern.compile("^(?=.*[A-Za-z0-9])[A-Za-z0-9\\s\\-/().,&']+$");
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final int DEFAULT_RADIUS_METERS = 100;
    private static final int MIN_RADIUS_METERS = 1;
    private static final int MAX_RADIUS_METERS = 10_000;

    private final LocationMasterRepository repository;
    private final LocationMasterMapper mapper;

    public LocationMasterServiceImpl(LocationMasterRepository repository, LocationMasterMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public LocationMasterDto create(LocationMasterDto request) {
        String locationName = normalizeLocationName(request.getLocationName());
        if (repository.existsByLocationNameIgnoreCase(locationName)) {
            throw new DuplicateResourceException("Address already exists: " + locationName);
        }

        LocationMaster entity = LocationMaster.builder()
                .departmentName(normalizeDepartmentName(request.getDepartmentName()))
                .locationName(locationName)
                .officeName(normalizeOfficeName(request.getOfficeName()))
                .latitude(validateLatitude(request.getLatitude()))
                .longitude(validateLongitude(request.getLongitude()))
                .radiusMeters(validateRadiusMeters(request.getRadiusMeters()))
                .activeFlag(normalizeActiveFlag(request.getActiveFlag()))
                .build();
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public LocationMasterDto update(Long locationId, LocationMasterDto request) {
        LocationMaster entity = findLocation(locationId);
        String locationName = normalizeLocationName(request.getLocationName());
        if (repository.existsByLocationNameIgnoreCaseAndLocationIdNot(locationName, locationId)) {
            throw new DuplicateResourceException("Address already exists: " + locationName);
        }

        entity.setDepartmentName(normalizeDepartmentName(request.getDepartmentName()));
        entity.setLocationName(locationName);
        entity.setOfficeName(normalizeOfficeName(request.getOfficeName()));
        entity.setLatitude(validateLatitude(request.getLatitude()));
        entity.setLongitude(validateLongitude(request.getLongitude()));
        entity.setRadiusMeters(validateRadiusMeters(request.getRadiusMeters()));
        entity.setActiveFlag(normalizeActiveFlag(request.getActiveFlag()));
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public LocationMasterDto getById(Long locationId) {
        return mapper.toDto(findLocation(locationId));
    }

    @Override
    public List<LocationMasterDto> getAll(boolean includeInactive) {
        List<LocationMaster> locations = includeInactive
                ? repository.findAllByOrderByLocationNameAsc()
                : repository.findByActiveFlagIgnoreCaseOrderByLocationNameAsc(ACTIVE);
        return locations.stream().map(mapper::toDto).toList();
    }

    @Override
    public Page<LocationMasterDto> search(boolean includeInactive, String searchText, Pageable pageable) {
        String normalizedSearchText = searchText == null ? "" : searchText.trim();
        Page<LocationMaster> locations;

        if (normalizedSearchText.isBlank()) {
            locations = includeInactive
                    ? repository.findAll(pageable)
                    : repository.findByActiveFlagIgnoreCase(ACTIVE, pageable);
        } else {
            locations = includeInactive
                    ? repository.searchByLocationNameOrOfficeName(normalizedSearchText, pageable)
                    : repository.searchByActiveFlagAndLocationNameOrOfficeName(
                            ACTIVE,
                            normalizedSearchText,
                            pageable);
        }

        return locations.map(mapper::toDto);
    }

    @Override
    @Transactional
    public void deactivate(Long locationId) {
        updateStatus(locationId, INACTIVE);
    }

    @Override
    @Transactional
    public void activate(Long locationId) {
        updateStatus(locationId, ACTIVE);
    }

    @Override
    @Transactional
    public void toggleStatus(Long locationId) {
        LocationMaster entity = findLocation(locationId);
        entity.setActiveFlag(ACTIVE.equalsIgnoreCase(entity.getActiveFlag()) ? INACTIVE : ACTIVE);
        repository.save(entity);
    }

    private void updateStatus(Long locationId, String activeFlag) {
        LocationMaster entity = findLocation(locationId);
        entity.setActiveFlag(activeFlag);
        repository.save(entity);
    }

    private LocationMaster findLocation(Long locationId) {
        return repository.findByLocationId(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + locationId));
    }

    private String normalizeLocationName(String locationName) {
        String normalized = locationName == null ? "" : locationName.trim();
        if (normalized.isBlank()) {
            throw new BusinessValidationException("Address is required");
        }
        if (normalized.length() > LOCATION_NAME_MAX_LENGTH) {
            throw new BusinessValidationException("Address must not exceed 150 characters");
        }
        if (!LOCATION_NAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessValidationException(
                    "Address can contain alphabets, numbers, spaces, hyphen, slash, brackets, dot and comma only");
        }
        return normalized;
    }

    private String normalizeDepartmentName(String departmentName) {
        String normalized = departmentName == null ? "" : departmentName.trim();
        if (normalized.isBlank()) {
            throw new BusinessValidationException("Department name is required");
        }
        if (normalized.length() > DEPARTMENT_NAME_MAX_LENGTH) {
            throw new BusinessValidationException("Department name must not exceed 100 characters");
        }
        return normalized;
    }

    private String normalizeOfficeName(String officeName) {
        String normalized = officeName == null ? "" : officeName.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > LOCATION_NAME_MAX_LENGTH) {
            throw new BusinessValidationException("Office name must not exceed 150 characters");
        }
        if (!OFFICE_NAME_PATTERN.matcher(normalized).matches()) {
            throw new BusinessValidationException(
                    "Office name can contain alphabets, numbers, spaces, hyphen, slash, brackets, dot, comma, ampersand and apostrophe only");
        }
        return normalized;
    }

    private BigDecimal validateLatitude(BigDecimal latitude) {
        return validateCoordinate(latitude, MIN_LATITUDE, MAX_LATITUDE, "Latitude");
    }

    private BigDecimal validateLongitude(BigDecimal longitude) {
        return validateCoordinate(longitude, MIN_LONGITUDE, MAX_LONGITUDE, "Longitude");
    }

    private BigDecimal validateCoordinate(BigDecimal value, BigDecimal minimum, BigDecimal maximum, String fieldName) {
        if (value == null) {
            throw new BusinessValidationException(fieldName + " is required");
        }
        if (value.scale() > 7) {
            throw new BusinessValidationException(fieldName + " can have up to 7 decimal places");
        }
        if (value.compareTo(minimum) < 0 || value.compareTo(maximum) > 0) {
            throw new BusinessValidationException(fieldName + " must be between " + minimum + " and " + maximum);
        }
        return value;
    }

    private String normalizeActiveFlag(String activeFlag) {
        if (activeFlag == null || activeFlag.isBlank()) {
            return ACTIVE;
        }
        return INACTIVE.equals(activeFlag.trim().toUpperCase(Locale.ROOT)) ? INACTIVE : ACTIVE;
    }

    private int validateRadiusMeters(Integer radiusMeters) {
        int resolvedRadiusMeters = radiusMeters == null ? DEFAULT_RADIUS_METERS : radiusMeters;
        if (resolvedRadiusMeters < MIN_RADIUS_METERS || resolvedRadiusMeters > MAX_RADIUS_METERS) {
            throw new BusinessValidationException(
                    "Area radius must be between " + MIN_RADIUS_METERS + " and " + MAX_RADIUS_METERS + " meters");
        }
        return resolvedRadiusMeters;
    }
}
