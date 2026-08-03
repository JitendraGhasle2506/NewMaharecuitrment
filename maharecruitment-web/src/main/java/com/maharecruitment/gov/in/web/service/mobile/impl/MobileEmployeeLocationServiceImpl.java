package com.maharecruitment.gov.in.web.service.mobile.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.maharecruitment.gov.in.master.entity.LocationMaster;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeEntity;
import com.maharecruitment.gov.in.recruitment.entity.EmployeeLocationMappingEntity;
import com.maharecruitment.gov.in.recruitment.repository.EmployeeLocationMappingRepository;
import com.maharecruitment.gov.in.web.dto.mobile.MobileEmployeeLocationResponse;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeAccessService;
import com.maharecruitment.gov.in.web.service.mobile.MobileEmployeeLocationService;

@Service
public class MobileEmployeeLocationServiceImpl implements MobileEmployeeLocationService {

    private final MobileEmployeeAccessService mobileEmployeeAccessService;
    private final EmployeeLocationMappingRepository employeeLocationMappingRepository;

    public MobileEmployeeLocationServiceImpl(
            MobileEmployeeAccessService mobileEmployeeAccessService,
            EmployeeLocationMappingRepository employeeLocationMappingRepository) {
        this.mobileEmployeeAccessService = mobileEmployeeAccessService;
        this.employeeLocationMappingRepository = employeeLocationMappingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public MobileEmployeeLocationResponse getMappedLocations(Long employeeId) {
        EmployeeEntity employee = mobileEmployeeAccessService.requireCurrentActiveEmployee(employeeId);
        List<MobileEmployeeLocationResponse.Location> locations = employeeLocationMappingRepository
                .findByEmployeeEmployeeIdOrderByPrimaryLocationDescLocationLocationNameAsc(employee.getEmployeeId())
                .stream()
                .filter(mapping -> Objects.nonNull(mapping.getLocation()))
                .filter(mapping -> isActive(mapping.getLocation()))
                .map(this::toLocationResponse)
                .toList();

        return new MobileEmployeeLocationResponse(
                true,
                "Mapped employee locations fetched successfully.",
                employee.getEmployeeId(),
                locations);
    }

    private MobileEmployeeLocationResponse.Location toLocationResponse(EmployeeLocationMappingEntity mapping) {
        LocationMaster location = mapping.getLocation();
        return new MobileEmployeeLocationResponse.Location(
                location.getLocationId(),
                textOrNull(location.getOfficeName()),
                textOrNull(location.getLocationName()),
                location.getLatitude(),
                location.getLongitude(),
                location.getRadiusMeters(),
                buildDisplayName(location),
                Boolean.TRUE.equals(mapping.getPrimaryLocation()));
    }

    private boolean isActive(LocationMaster location) {
        return "Y".equalsIgnoreCase(location.getActiveFlag());
    }

    private String buildDisplayName(LocationMaster location) {
        String officeName = textOrNull(location.getOfficeName());
        String locationName = defaultText(location.getLocationName(), "-");
        return StringUtils.hasText(officeName) ? officeName + " - " + locationName : locationName;
    }

    private String defaultText(String value, String fallback) {
        String normalized = textOrNull(value);
        return normalized != null ? normalized : fallback;
    }

    private String textOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
