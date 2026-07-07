package com.maharecruitment.gov.in.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskingUtilTest {

    @Test
    void masksAadhaarWithOnlyLastFourDigitsVisible() {
        assertThat(SensitiveDataMaskingUtil.maskAadhaar("123456783181")).isEqualTo("XXXXXXXX3181");
    }

    @Test
    void masksFormattedAadhaarByUsingDigitsOnly() {
        assertThat(SensitiveDataMaskingUtil.maskAadhaar("1234 5678 3181")).isEqualTo("XXXXXXXX3181");
    }

    @Test
    void returnsNullForBlankAadhaar() {
        assertThat(SensitiveDataMaskingUtil.maskAadhaar(null)).isNull();
        assertThat(SensitiveDataMaskingUtil.maskAadhaar(" ")).isNull();
    }
}
