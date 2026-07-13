package com.maharecruitment.gov.in.common.sms.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MobileNumberUtilTest {

    @Test
    void normalizesIndianMobileNumbersToTenDigits() {
        assertThat(MobileNumberUtil.normalizeIndianMobileNumber("+91 70201-86501")).isEqualTo("7020186501");
        assertThat(MobileNumberUtil.normalizeIndianMobileNumber("917020186501")).isEqualTo("7020186501");
        assertThat(MobileNumberUtil.normalizeIndianMobileNumber("7020186501")).isEqualTo("7020186501");
    }

    @Test
    void masksMobileNumber() {
        assertThat(MobileNumberUtil.mask("+91 7020186501")).isEqualTo("******6501");
    }

    @Test
    void rejectsInvalidMobileNumbers() {
        assertThatThrownBy(() -> MobileNumberUtil.normalizeIndianMobileNumber("5020186501"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Indian mobile number");
        assertThatThrownBy(() -> MobileNumberUtil.normalizeIndianMobileNumber("70201ABCDE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid Indian mobile number");
    }
}
