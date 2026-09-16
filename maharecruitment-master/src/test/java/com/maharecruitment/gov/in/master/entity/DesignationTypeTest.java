package com.maharecruitment.gov.in.master.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DesignationTypeTest {

    @Test
    void exposesRequiredCodesAndLabels() {
        assertThat(DesignationType.O.getDisplayName()).isEqualTo("Other");
        assertThat(DesignationType.M.getDisplayName()).isEqualTo("MAHAIT On Roll");
    }
}
