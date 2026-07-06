package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class SecurityTransportConfigurationTest {

    @Test
    void baseConfigurationUsesWildFlyHttpSafeDefaults() throws Exception {
        Properties properties = loadProperties("application.properties");

        assertThat(properties.getProperty("app.security.cookie.secure"))
                .isEqualTo("${APP_SECURITY_COOKIE_SECURE:true}");
        assertThat(properties.getProperty("app.security.cookie.http-only")).isEqualTo("true");
        assertThat(properties.getProperty("app.security.cookie.same-site")).isEqualTo("Lax");
        assertThat(properties.getProperty("server.servlet.session.cookie.secure"))
                .isEqualTo("${app.security.cookie.secure}");
        assertThat(properties.getProperty("server.servlet.session.cookie.http-only"))
                .isEqualTo("${app.security.cookie.http-only}");
        assertThat(properties.getProperty("server.forward-headers-strategy")).isEqualTo("none");
        assertThat(properties.getProperty("spring.mvc.log-request-details")).isEqualTo("false");
        assertThat(properties.getProperty("app.security.transport.require-https"))
                .isEqualTo("${APP_SECURITY_REQUIRE_HTTPS:false}");
        assertThat(properties.getProperty("app.security.transport.allow-loopback-http")).isEqualTo("false");
        assertThat(properties.getProperty("app.security.transport.trust-forwarded-headers"))
                .isEqualTo("${APP_SECURITY_TRUST_FORWARDED_HEADERS:false}");
        assertThat(properties.getProperty("app.base-url"))
                .isEqualTo("${APP_BASE_URL:https://portal.example.gov.in/maharecruitment-web}");
        assertThat(properties.getProperty("app.mobile-auth.issuer"))
                .isEqualTo("${MOBILE_AUTH_JWT_ISSUER:maharecruitment-mobile}");
        assertThat(properties.getProperty("security.host-validation.enabled"))
                .isEqualTo("${SECURITY_HOST_VALIDATION_ENABLED:false}");
        assertThat(properties.getProperty("security.allowed-hosts[0]"))
                .isEqualTo("${SECURITY_ALLOWED_HOST_PORTAL:103.5.84.215}");
        assertThat(properties.getProperty("security.allowed-hosts[4]"))
                .isEqualTo("${SECURITY_ALLOWED_HOST_SERVER:}");
        assertThat(properties.getProperty("security.allowed-ports[1]"))
                .isEqualTo("${SECURITY_ALLOWED_PORT_HTTPS:443}");
        assertThat(properties.getProperty("security.allowed-ports[4]"))
                .isEqualTo("${SECURITY_ALLOWED_PORT_WILDFLY_HTTP:8080}");
        assertThat(properties.getProperty("otp.resend-limit")).isEqualTo("3");
        assertThat(properties.getProperty("otp.resend-window-minutes")).isEqualTo("5");
        assertThat(properties.getProperty("spring.profiles.active")).isNull();
        assertThat(properties.getProperty("spring.profiles.default")).isEqualTo("local");
        assertThat(properties.getProperty("server.servlet.context-path"))
                .isEqualTo("${SERVER_SERVLET_CONTEXT_PATH:/maharecruitment}");
    }

    @Test
    void deployedProfilesAllowHttpOnlyWildFlyDeploymentWithoutCertificate() throws Exception {
        for (String fileName : List.of(
                "application-uat.properties",
                "application-prod.properties")) {
            Properties properties = loadProperties(fileName);

            assertThat(properties.getProperty("app.security.cookie.secure"))
                    .as(fileName)
                    .isEqualTo("${APP_SECURITY_COOKIE_SECURE:true}");
            assertThat(properties.getProperty("app.security.transport.require-https"))
                    .as(fileName)
                    .isEqualTo("${APP_SECURITY_REQUIRE_HTTPS:false}");
            assertThat(properties.getProperty("app.security.transport.allow-loopback-http"))
                    .as(fileName)
                    .isEqualTo("false");
            assertThat(properties.getProperty("app.security.transport.trust-forwarded-headers"))
                    .as(fileName)
                    .isEqualTo("${APP_SECURITY_TRUST_FORWARDED_HEADERS:false}");
            assertThat(properties.getProperty("security.host-validation.enabled"))
                    .as(fileName)
                    .isEqualTo("${SECURITY_HOST_VALIDATION_ENABLED:false}");
        }
    }

    @Test
    void localProfileAllowsHttpLoopbackDevelopment() throws Exception {
        Properties properties = loadProperties("application-local.properties");

        assertThat(properties.getProperty("server.port")).isEqualTo("8777");
        assertThat(properties.getProperty("server.ssl.enabled")).isEqualTo("false");
        assertThat(properties.getProperty("app.security.cookie.secure")).isEqualTo("false");
        assertThat(properties.getProperty("app.security.transport.require-https")).isEqualTo("false");
        assertThat(properties.getProperty("app.security.transport.allow-loopback-http")).isEqualTo("true");
        assertThat(properties.getProperty("app.security.transport.trust-forwarded-headers")).isEqualTo("false");
        assertThat(properties.getProperty("app.security.transport.http-port")).isEqualTo("8777");
        assertThat(properties.getProperty("app.security.transport.https-port")).isEqualTo("8443");
        assertThat(properties.getProperty("app.base-url"))
                .isEqualTo("${APP_BASE_URL:https://localhost:8443/maharecruitment}");
        assertThat(properties.getProperty("security.host-validation.enabled"))
                .isEqualTo("${SECURITY_HOST_VALIDATION_ENABLED:true}");
        assertThat(properties.getProperty("app.security.local-http-redirect.enabled")).isEqualTo("false");
    }

    private Properties loadProperties(String fileName) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(resourcesDirectory().resolve(fileName))) {
            properties.load(reader);
        }
        return properties;
    }

    private Path resourcesDirectory() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isDirectory(userDir.resolve("src/main/resources"))) {
            return userDir.resolve("src/main/resources");
        }
        return userDir.resolve("maharecruitment-web/src/main/resources");
    }
}
