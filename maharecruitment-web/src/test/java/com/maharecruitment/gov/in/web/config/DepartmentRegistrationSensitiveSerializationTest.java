package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maharecruitment.gov.in.auth.entity.DepartmentRegistrationEntity;
import org.junit.jupiter.api.Test;

class DepartmentRegistrationSensitiveSerializationTest {

    @Test
    void entitySerializationAndToStringExcludeSensitiveStorageFields() throws Exception {
        DepartmentRegistrationEntity entity = new DepartmentRegistrationEntity();
        entity.setDepartmentRegistrationId(101L);
        entity.setGstNo("27ABCDE1234F1Z5");
        entity.setPanNo("ABCDE2546F");

        String json = new ObjectMapper().writeValueAsString(entity);
        String text = entity.toString();
        assertThat(json)
                .doesNotContain("27ABCDE1234F1Z5", "ABCDE2546F")
                .doesNotContain("gstNo", "panNo");
        assertThat(text)
                .doesNotContain("27ABCDE1234F1Z5", "ABCDE2546F");
    }
}
