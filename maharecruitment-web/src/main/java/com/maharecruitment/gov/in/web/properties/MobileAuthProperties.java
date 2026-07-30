package com.maharecruitment.gov.in.web.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConfigurationProperties(prefix = "app.mobile-auth")
public class MobileAuthProperties {

    private static final Duration DEFAULT_ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration DEFAULT_REFRESH_TOKEN_TTL = Duration.ofDays(30);

    private String issuer = "maharecruitment-mobile";

    private Duration accessTokenTtl = DEFAULT_ACCESS_TOKEN_TTL;
    private Duration refreshTokenTtl = DEFAULT_REFRESH_TOKEN_TTL;

    private String jwtSecret;

    public String getIssuer() {
        return StringUtils.hasText(issuer) ? issuer.trim() : "maharecruitment-mobile";
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()
                ? DEFAULT_ACCESS_TOKEN_TTL
                : accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()
                ? DEFAULT_REFRESH_TOKEN_TTL
                : refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }
}
