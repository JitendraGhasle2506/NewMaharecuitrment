package com.maharecruitment.gov.in.master.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.maharecruitment.gov.in.master.config.AgencyMasterProperties;
import com.maharecruitment.gov.in.master.service.AgencyTypeCatalog;

@Service
public class ConfiguredAgencyTypeCatalog implements AgencyTypeCatalog {

    private final AgencyMasterProperties properties;

    public ConfiguredAgencyTypeCatalog(AgencyMasterProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> getAllowedTypes() {
        return properties.getAllowedTypes().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    @Override
    public boolean isSupported(String agencyType) {
        return findCanonicalType(agencyType).isPresent();
    }

    @Override
    public String resolveCanonicalType(String agencyType) {
        return findCanonicalType(agencyType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MSG704: Agency type must be selected from the allowed list."));
    }

    private Optional<String> findCanonicalType(String agencyType) {
        if (agencyType == null || agencyType.isBlank()) {
            return Optional.empty();
        }

        String normalized = agencyType.trim().toLowerCase(Locale.ROOT);
        return getAllowedTypes().stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }
}
