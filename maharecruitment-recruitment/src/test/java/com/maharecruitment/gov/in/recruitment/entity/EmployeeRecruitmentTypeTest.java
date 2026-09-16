package com.maharecruitment.gov.in.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmployeeRecruitmentTypeTest {

    @Test
    void normalizesEverySupportedEmployeeType() {
        assertThat(EmployeeRecruitmentType.normalizeOrNull(" internal ")).isEqualTo("INTERNAL");
        assertThat(EmployeeRecruitmentType.normalizeOrNull("external")).isEqualTo("EXTERNAL");
        assertThat(EmployeeRecruitmentType.normalizeOrNull("mahait")).isEqualTo("MAHAIT");
    }

    @Test
    void rejectsBlankAndUnsupportedEmployeeTypes() {
        assertThat(EmployeeRecruitmentType.normalizeOrNull(null)).isNull();
        assertThat(EmployeeRecruitmentType.normalizeOrNull(" ")).isNull();
        assertThat(EmployeeRecruitmentType.normalizeOrNull("CONTRACTOR")).isNull();
    }
}
