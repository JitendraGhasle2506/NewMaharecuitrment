package com.maharecruitment.gov.in.master.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agency.master")
public class AgencyMasterProperties {

    private List<String> allowedTypes = new ArrayList<>(List.of(
            "Manpower Supply Agency",
            "Recruitment Agency",
            "Outsourcing Agency",
            "Consultancy Agency"));

    public List<String> getAllowedTypes() {
        return allowedTypes;
    }

    public void setAllowedTypes(List<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }
}
