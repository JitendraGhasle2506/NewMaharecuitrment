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
    void normalizesAadhaarToDigitsForSecureReveal() {
        assertThat(SensitiveDataMaskingUtil.normalizeAadhaar("1234-5678 3181")).isEqualTo("123456783181");
    }

    @Test
    void returnsNullForBlankAadhaar() {
        assertThat(SensitiveDataMaskingUtil.maskAadhaar(null)).isNull();
        assertThat(SensitiveDataMaskingUtil.maskAadhaar(" ")).isNull();
    }

    @Test
    void masksPanAndGstKeepingOnlyLastFour() {
        assertThat(SensitiveDataMaskingUtil.maskPan("ABCDE2546F")).isEqualTo("XXXXXX546F");
        assertThat(SensitiveDataMaskingUtil.maskGst("27ABCDE1234F1Z5")).isEqualTo("XXXXXXXXXXXF1Z5");
    }

    @Test
    void neverExposesShortValues() {
        assertThat(SensitiveDataMaskingUtil.maskKeepingLastFour("1234")).isEqualTo("XXXX");
        assertThat(SensitiveDataMaskingUtil.maskKeepingLastFour("12")).isEqualTo("XX");
    }
}
