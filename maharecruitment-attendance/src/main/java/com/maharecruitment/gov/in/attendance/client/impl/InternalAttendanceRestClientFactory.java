package com.maharecruitment.gov.in.attendance.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;

final class InternalAttendanceRestClientFactory {

    private static final Logger log = LoggerFactory.getLogger(InternalAttendanceRestClientFactory.class);
    private static final String TLS_PROTOCOL = "TLS";

    private InternalAttendanceRestClientFactory() {
    }

    static RestClient create(InternalAttendanceSyncProperties properties) {
        Duration connectTimeout = Duration.ofSeconds(Math.max(properties.getConnectTimeoutSeconds(), 1));
        Duration readTimeout = Duration.ofSeconds(Math.max(properties.getReadTimeoutSeconds(), 1));

        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(connectTimeout);

        String certificateLocation = properties.getAdditionalCaCertificate();
        if (StringUtils.hasText(certificateLocation)) {
            certificateLocation = certificateLocation.trim();
            httpClientBuilder.sslContext(createSslContext(certificateLocation));
            log.info(
                    "Configured internal attendance HTTP client with JDK transport and additional CA certificate. certificateLocation={}",
                    certificateLocation);
        } else {
            log.info("Configured internal attendance HTTP client with JDK transport and JVM trust store.");
        }

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClientBuilder.build());
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private static SSLContext createSslContext(String certificateLocation) {
        try {
            X509TrustManager systemTrustManager = createTrustManager(null);
            X509TrustManager additionalTrustManager = createAdditionalTrustManager(certificateLocation);
            SSLContext sslContext = SSLContext.getInstance(TLS_PROTOCOL);
            sslContext.init(
                    null,
                    new TrustManager[] {
                            new FallbackX509TrustManager(systemTrustManager, additionalTrustManager)
                    },
                    null);
            return sslContext;
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException(
                    "Unable to load the internal attendance API CA certificate from "
                            + certificateLocation
                            + ".",
                    ex);
        }
    }

    static X509TrustManager createAdditionalTrustManager(String certificateLocation)
            throws GeneralSecurityException, IOException {
        Resource resource = new DefaultResourceLoader().getResource(certificateLocation);
        if (!resource.exists()) {
            throw new IOException("CA certificate resource does not exist: " + certificateLocation);
        }

        Collection<? extends Certificate> certificates;
        try (InputStream inputStream = resource.getInputStream()) {
            certificates = CertificateFactory.getInstance("X.509")
                    .generateCertificates(inputStream);
        }
        if (certificates.isEmpty()) {
            throw new CertificateException("CA certificate resource is empty: " + certificateLocation);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        int certificateIndex = 0;
        for (Certificate certificate : certificates) {
            X509Certificate x509Certificate = (X509Certificate) certificate;
            if (x509Certificate.getBasicConstraints() < 0) {
                throw new CertificateException(
                        "The configured attendance trust certificate is not a CA certificate: "
                                + x509Certificate.getSubjectX500Principal());
            }
            trustStore.setCertificateEntry("attendance-ca-" + certificateIndex++, x509Certificate);
        }
        return createTrustManager(trustStore);
    }

    private static X509TrustManager createTrustManager(KeyStore trustStore)
            throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }
        throw new GeneralSecurityException("No X.509 trust manager is available.");
    }

    private static final class FallbackX509TrustManager implements X509TrustManager {

        private final X509TrustManager systemTrustManager;
        private final X509TrustManager additionalTrustManager;

        private FallbackX509TrustManager(
                X509TrustManager systemTrustManager,
                X509TrustManager additionalTrustManager) {
            this.systemTrustManager = systemTrustManager;
            this.additionalTrustManager = additionalTrustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            systemTrustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                systemTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException systemFailure) {
                try {
                    additionalTrustManager.checkServerTrusted(chain, authType);
                } catch (CertificateException additionalFailure) {
                    additionalFailure.addSuppressed(systemFailure);
                    throw additionalFailure;
                }
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] systemIssuers = systemTrustManager.getAcceptedIssuers();
            X509Certificate[] additionalIssuers = additionalTrustManager.getAcceptedIssuers();
            X509Certificate[] acceptedIssuers = new X509Certificate[
                    systemIssuers.length + additionalIssuers.length];
            System.arraycopy(systemIssuers, 0, acceptedIssuers, 0, systemIssuers.length);
            System.arraycopy(
                    additionalIssuers,
                    0,
                    acceptedIssuers,
                    systemIssuers.length,
                    additionalIssuers.length);
            return acceptedIssuers;
        }
    }
}
