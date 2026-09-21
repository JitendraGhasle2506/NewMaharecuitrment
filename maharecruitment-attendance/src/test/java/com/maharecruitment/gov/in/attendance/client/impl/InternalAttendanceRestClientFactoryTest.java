package com.maharecruitment.gov.in.attendance.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;

import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;

class InternalAttendanceRestClientFactoryTest {

    @Test
    void bundledCertificateIsOfficialIsrgRootX1() throws Exception {
        X509TrustManager trustManager = InternalAttendanceRestClientFactory
                .createAdditionalTrustManager("classpath:certificates/isrg-root-x1.pem");

        assertThat(trustManager.getAcceptedIssuers()).singleElement().satisfies(certificate -> {
            assertThat(certificate.getSubjectX500Principal().getName())
                    .isEqualTo("CN=ISRG Root X1,O=Internet Security Research Group,C=US");
            assertThat(sha256(certificate))
                    .isEqualTo("96BCEC06264976F37460779ACF28C5A7CFE8A3C0AAE11A8FFCEE05C0BDDF08C6");
        });
    }

    @Test
    void blankCertificateOverrideFallsBackToBundledCertificate() {
        InternalAttendanceSyncProperties properties = new InternalAttendanceSyncProperties();

        properties.setAdditionalCaCertificate("   ");

        assertThat(properties.getAdditionalCaCertificate())
                .isEqualTo("classpath:certificates/isrg-root-x1.pem");
    }

    @Test
    void certificateValidationIsDisabledOnlyWhenExplicitlyConfigured() {
        InternalAttendanceSyncProperties properties = new InternalAttendanceSyncProperties();

        assertThat(properties.isTrustAllCertificates()).isFalse();

        properties.setTrustAllCertificates(true);

        assertThat(properties.isTrustAllCertificates()).isTrue();
    }

    private String sha256(X509Certificate certificate) {
        try {
            byte[] fingerprint = MessageDigest.getInstance("SHA-256")
                    .digest(certificate.getEncoded());
            return HexFormat.of().withUpperCase().formatHex(fingerprint);
        } catch (Exception ex) {
            throw new AssertionError("Unable to calculate the certificate fingerprint.", ex);
        }
    }
}
