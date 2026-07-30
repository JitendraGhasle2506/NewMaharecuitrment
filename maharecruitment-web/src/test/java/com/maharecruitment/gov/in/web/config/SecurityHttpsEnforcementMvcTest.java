package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.maharecruitment.gov.in.auth.handler.MySimpleUrlAuthenticationSuccessHandler;
import com.maharecruitment.gov.in.common.security.CookieSecurityProperties;
import com.maharecruitment.gov.in.security.handler.CustomAccessDeniedHandler;
import com.maharecruitment.gov.in.security.handler.CustomLoginFailureHandler;
import com.maharecruitment.gov.in.security.handler.CustomLogoutSuccessHandler;
import com.maharecruitment.gov.in.web.controller.HomeController;
import com.maharecruitment.gov.in.web.filter.CookieAttributeFilter;
import com.maharecruitment.gov.in.web.filter.MobileBearerTokenAuthenticationFilter;
import com.maharecruitment.gov.in.web.properties.NotificationChannelProperties;
import com.maharecruitment.gov.in.web.properties.OtpVerificationProperties;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.security.headers.SecurityHeaderPolicy;
import com.maharecruitment.gov.in.web.security.host.HostProperties;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;
import com.maharecruitment.gov.in.web.service.mobile.MobileTokenService;
import com.maharecruitment.gov.in.web.util.ContextPathUrlResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SpringJUnitWebConfig
@ContextConfiguration(classes = SecurityHttpsEnforcementMvcTest.TestConfig.class)
class SecurityHttpsEnforcementMvcTest {

    private static final String HOST = "portal.example.gov.in";

    @Autowired
    private org.springframework.web.context.WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void httpLoginRequestRedirectsToHttps() throws Exception {
        mockMvc.perform(get("/login").header("Host", HOST).secure(false))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://")))
                .andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    @Test
    void httpsLoginResponseContainsHstsHeader() throws Exception {
        MvcResult result = mockMvc.perform(get("/login").header("Host", HOST).secure(true))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getHeader("Strict-Transport-Security"))
                .contains("max-age=31536000")
                .contains("includeSubDomains")
                .doesNotContain("preload");
    }

    @Test
    void webResponseContainsRequiredSecurityHeaders() throws Exception {
        mockMvc.perform(get("/login").header("Host", HOST).secure(true))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", SecurityHeaderPolicy.PERMISSIONS_POLICY))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        SecurityHeaderPolicy.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("X-XSS-Protection", "0"));
    }

    @Test
    void apiErrorResponseContainsRequiredSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/mobile/profile").header("Host", HOST).secure(true))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", SecurityHeaderPolicy.PERMISSIONS_POLICY))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        SecurityHeaderPolicy.CONTENT_SECURITY_POLICY));
    }

    @Test
    void invoiceFrameResponseRetainsNarrowSameOriginCompatibilityPolicy() throws Exception {
        mockMvc.perform(get("/invoice/tax-invoices/application/42/preview/new")
                        .param("embedded", "true")
                        .header("Host", HOST)
                        .secure(true))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        SecurityHeaderPolicy.SAME_ORIGIN_FRAME_CONTENT_SECURITY_POLICY));
    }

    @Test
    void invalidHostIsRejectedBeforeLoginPage() throws Exception {
        mockMvc.perform(get("/login").header("Host", "evil.example.com").secure(true))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Permissions-Policy", SecurityHeaderPolicy.PERMISSIONS_POLICY))
                .andExpect(header().string(
                        "Content-Security-Policy",
                        SecurityHeaderPolicy.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("Strict-Transport-Security",
                        org.hamcrest.Matchers.containsString("max-age=31536000")));
    }

    @Configuration
    @EnableWebMvc
    @Import({ SecurityConfig.class, CookieAttributeFilter.class })
    static class TestConfig {

        @Bean
        HomeController homeController() {
            return new HomeController(
                    otpVerificationProperties(),
                    notificationChannelProperties(),
                    contextPathUrlResolver());
        }

        @Bean
        ContextPathUrlResolver contextPathUrlResolver() {
            return new ContextPathUrlResolver();
        }

        @Bean
        OtpVerificationProperties otpVerificationProperties() {
            return new OtpVerificationProperties();
        }

        @Bean
        NotificationChannelProperties notificationChannelProperties() {
            return new NotificationChannelProperties();
        }

        @Bean
        TransportSecurityProperties transportSecurityProperties() {
            TransportSecurityProperties properties = new TransportSecurityProperties();
            properties.setAllowLoopbackHttp(false);
            return properties;
        }

        @Bean
        CookieSecurityProperties cookieSecurityProperties() {
            CookieSecurityProperties properties = new CookieSecurityProperties();
            properties.setSecure(true);
            properties.setHttpOnly(true);
            properties.setSameSite("Lax");
            return properties;
        }

        @Bean
        HostProperties hostProperties() {
            HostProperties properties = new HostProperties();
            properties.setAllowedHosts(List.of("localhost", "127.0.0.1", "portal.example.gov.in"));
            properties.setAllowedPorts(Set.of(80, 443, 8443, 8777));
            return properties;
        }

        @Bean
        UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
            return username -> User.withUsername(username)
                    .password(passwordEncoder.encode("Password123!"))
                    .roles("USER")
                    .build();
        }

        @Bean
        MobileTokenService mobileTokenService() {
            return mock(MobileTokenService.class);
        }

        @Bean
        AgencyAccessService agencyAccessService() {
            return mock(AgencyAccessService.class);
        }

        @Bean
        MobileBearerTokenAuthenticationFilter mobileBearerTokenAuthenticationFilter(
                MobileTokenService mobileTokenService,
                UserDetailsService userDetailsService) {
            return new MobileBearerTokenAuthenticationFilter(mobileTokenService, userDetailsService);
        }

        @Bean
        MySimpleUrlAuthenticationSuccessHandler successHandler() {
            return mock(MySimpleUrlAuthenticationSuccessHandler.class);
        }

        @Bean
        CustomLoginFailureHandler loginFailureHandler() {
            return mock(CustomLoginFailureHandler.class);
        }

        @Bean
        CustomAccessDeniedHandler accessDeniedHandler() {
            return mock(CustomAccessDeniedHandler.class);
        }

        @Bean
        CustomLogoutSuccessHandler logoutSuccessHandler() {
            return mock(CustomLogoutSuccessHandler.class);
        }

        @Bean
        ViewResolver viewResolver() {
            return (viewName, locale) -> new NoOpView();
        }
    }

    private static final class NoOpView implements View {

        @Override
        public String getContentType() {
            return "text/html";
        }

        @Override
        public void render(
                java.util.Map<String, ?> model,
                HttpServletRequest request,
                HttpServletResponse response) throws Exception {
            response.setContentType(getContentType());
            response.getWriter().write("ok");
        }
    }
}
