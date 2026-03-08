package com.maharecruitment.gov.in.master.service;

import java.util.List;

public interface AgencyTypeCatalog {

    List<String> getAllowedTypes();

    boolean isSupported(String agencyType);

    String resolveCanonicalType(String agencyType);
}
