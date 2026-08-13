package com.maharecruitment.gov.in.web.config;

import org.springframework.core.Ordered;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter.XFrameOptionsMode;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.maharecruitment.gov.in.security.handler.CustomAccessDeniedHandler;
import com.maharecruitment.gov.in.security.handler.CustomLoginFailureHandler;
import com.maharecruitment.gov.in.security.handler.CustomLogoutSuccessHandler;
import com.maharecruitment.gov.in.web.filter.AgencyAccountStatusFilter;
import com.maharecruitment.gov.in.web.filter.CookieAttributeFilter;
import com.maharecruitment.gov.in.web.filter.HttpMethodPolicyFilter;
import com.maharecruitment.gov.in.web.filter.MobileBearerTokenAuthenticationFilter;
import com.maharecruitment.gov.in.web.filter.SecurityResponseHeadersFilter;
import com.maharecruitment.gov.in.web.properties.TransportSecurityProperties;
import com.maharecruitment.gov.in.web.security.headers.SecurityHeaderPolicy;
import com.maharecruitment.gov.in.web.security.host.HostHeaderValidationFilter;
import com.maharecruitment.gov.in.web.security.host.HostProperties;
import com.maharecruitment.gov.in.web.security.host.HostValidator;
import com.maharecruitment.gov.in.web.service.agency.AgencyAccessService;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
     static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    HostValidator hostValidator(HostProperties hostProperties) {
        return new HostValidator(hostProperties);
    }

    @Bean
    HostHeaderValidationFilter hostHeaderValidationFilter(HostValidator hostValidator) {
        return new HostHeaderValidationFilter(hostValidator);
    }

    @Bean
    HttpMethodPolicyFilter httpMethodPolicyFilter(
            @org.springframework.beans.factory.annotation.Value(
                    "${app.security.http-methods.allow-options:false}") boolean allowOptions) {
        return new HttpMethodPolicyFilter(allowOptions);
    }

    @Bean
    FilterRegistrationBean<HttpMethodPolicyFilter> httpMethodPolicyFilterRegistration(
            HttpMethodPolicyFilter httpMethodPolicyFilter) {
        FilterRegistrationBean<HttpMethodPolicyFilter> registration = new FilterRegistrationBean<>(httpMethodPolicyFilter);
        // Spring Security owns ordering; prevent servlet-container double registration.
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    AgencyAccountStatusFilter agencyAccountStatusFilter(AgencyAccessService agencyAccessService) {
        return new AgencyAccountStatusFilter(agencyAccessService);
    }

    @Bean
    FilterRegistrationBean<AgencyAccountStatusFilter> agencyAccountStatusFilterRegistration(
            AgencyAccountStatusFilter agencyAccountStatusFilter) {
        FilterRegistrationBean<AgencyAccountStatusFilter> registration = new FilterRegistrationBean<>(
                agencyAccountStatusFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<HostHeaderValidationFilter> hostHeaderValidationFilterRegistration(
            HostHeaderValidationFilter hostHeaderValidationFilter) {
        FilterRegistrationBean<HostHeaderValidationFilter> registration = new FilterRegistrationBean<>(
                hostHeaderValidationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityResponseHeadersFilter securityResponseHeadersFilter() {
        return new SecurityResponseHeadersFilter();
    }

    @Bean
    FilterRegistrationBean<SecurityResponseHeadersFilter> securityResponseHeadersFilterRegistration(
            SecurityResponseHeadersFilter securityResponseHeadersFilter) {
        FilterRegistrationBean<SecurityResponseHeadersFilter> registration = new FilterRegistrationBean<>(
                securityResponseHeadersFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(
                DispatcherType.REQUEST,
                DispatcherType.FORWARD,
                DispatcherType.ERROR,
                DispatcherType.INCLUDE,
                DispatcherType.ASYNC);
        return registration;
    }

    @Bean
    FilterRegistrationBean<CookieAttributeFilter> cookieAttributeFilterRegistration(
            CookieAttributeFilter cookieAttributeFilter) {
        FilterRegistrationBean<CookieAttributeFilter> registration = new FilterRegistrationBean<>(
                cookieAttributeFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/*");
        registration.setDispatcherTypes(
                DispatcherType.REQUEST,
                DispatcherType.FORWARD,
                DispatcherType.ERROR,
                DispatcherType.INCLUDE,
                DispatcherType.ASYNC);
        return registration;
    }

    @Bean
    FilterRegistrationBean<MobileBearerTokenAuthenticationFilter> mobileBearerTokenAuthenticationFilterRegistration(
            MobileBearerTokenAuthenticationFilter mobileBearerTokenAuthenticationFilter) {
        FilterRegistrationBean<MobileBearerTokenAuthenticationFilter> registration = new FilterRegistrationBean<>(
                mobileBearerTokenAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            TransportSecurityProperties transportSecurityProperties,
            HostHeaderValidationFilter hostHeaderValidationFilter,
            HttpMethodPolicyFilter httpMethodPolicyFilter,
            MobileBearerTokenAuthenticationFilter mobileBearerTokenAuthenticationFilter,
            AgencyAccountStatusFilter agencyAccountStatusFilter,
            com.maharecruitment.gov.in.auth.handler.MySimpleUrlAuthenticationSuccessHandler successHandler,
            CustomLoginFailureHandler loginFailureHandler,
            CustomAccessDeniedHandler accessDeniedHandler,
            CustomLogoutSuccessHandler logoutSuccessHandler) throws Exception {

        http.authenticationProvider(authenticationProvider);
        // Run the method allowlist before redirects, authentication and authorization.
        http.addFilterBefore(httpMethodPolicyFilter, ChannelProcessingFilter.class);
        http.addFilterBefore(hostHeaderValidationFilter, ChannelProcessingFilter.class);
        http.addFilterBefore(mobileBearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(agencyAccountStatusFilter, UsernamePasswordAuthenticationFilter.class);

        http
            // 🔥 (Optional) disable CSRF temporarily if needed
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/mobile/**"));

        if (transportSecurityProperties.hasPortMapping()) {
            http.portMapper(portMapper -> portMapper
                    .http(transportSecurityProperties.getHttpPort())
                    .mapsTo(transportSecurityProperties.getHttpsPort()));
        }

        http
            .requiresChannel(channel -> {
                if (transportSecurityProperties.isRequireHttps()) {
                    channel.requestMatchers(new HttpsRequiredRequestMatcher(transportSecurityProperties))
                            .requiresSecure();
                }
            })

            .authorizeHttpRequests(auth -> auth

                // ✅ IMPORTANT: keep login FIRST
                .requestMatchers("/login", "/doLogin", "/login/otp", "/login/otp/send", "/forgot-password", "/forgot-password/**").permitAll()
                .requestMatchers("/api/mobile/auth/login").permitAll()
                .requestMatchers("/api/mobile/auth/refresh").permitAll()
                .requestMatchers("/api/mobile/auth/logout").permitAll()
                .requestMatchers("/api/mobile/auth/password-reset/**").permitAll()
                .requestMatchers("/security/credential-encryption/public-key").permitAll()
                .requestMatchers("/api/verifications/otp/**").permitAll()

                .requestMatchers(
                        "/", "/index", "/register/**",
                        "/registration**", "/js/**", "/css/**", "/assets/**",
                        "/img/**", "/images/**", "/icons/**", "/webjars/**",
                        "/test/**", "/otp/**",
                        "/error", "/error/**"
                ).permitAll()

                .requestMatchers("/api/mobile/**").authenticated()

                .requestMatchers("/common/mahait-profile/**", "/common/holidays/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")

                .requestMatchers("/home", "/common/**").authenticated()
                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")

                .requestMatchers("/hr/department/payment/**")
                    .hasAnyAuthority("ROLE_HR", "ROLE_AUDITOR")

                .requestMatchers("/hr/**", "/employees/**").hasAuthority("ROLE_HR")
                .requestMatchers("/agency/**").hasAuthority("ROLE_AGENCY")
                .requestMatchers("/user/**").hasAuthority("ROLE_USER")

                .requestMatchers("/panel/**")
                    .hasAnyAuthority("ROLE_COO", "ROLE_HOD", "ROLE_HOD1", "ROLE_STM", "ROLE_HR", "ROLE_PM", "ROLE_EMPLOYEE")

                .requestMatchers("/interview-authority/**")
                    .hasAnyAuthority("ROLE_HOD", "ROLE_PM", "ROLE_STM", "ROLE_EMPLOYEE")

                .requestMatchers("/stm/**").hasAuthority("ROLE_STM")
                .requestMatchers("/pm/**").hasAuthority("ROLE_PM")
                .requestMatchers("/hod1/**", "/hod2/**").hasAuthority("ROLE_HOD")

                .requestMatchers("/coo/**")
                    .hasAnyAuthority("ROLE_COO", "ROLE_AUDITOR")

                .requestMatchers("/md/**").hasAuthority("ROLE_MD")

                .requestMatchers("/employee/**").hasAuthority("ROLE_EMPLOYEE")

                .requestMatchers("/department/payment/*/receipt")
                    .hasAnyAuthority("ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AUDITOR")

                .requestMatchers("/invoice/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AUDITOR")

                .requestMatchers("/department/**").hasAuthority("ROLE_DEPARTMENT")
                .requestMatchers("/auditor/**").hasAuthority("ROLE_AUDITOR")

                .requestMatchers("/attendance/**", "/eservicebook/**", "/pension/**",
                                 "/hrms/**", "/payroll/**")
                    .hasAuthority("ROLE_ADMIN")

                .anyRequest().authenticated()
            )

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/doLogin")
                .successHandler(successHandler)
                .failureHandler(loginFailureHandler)
                .permitAll()
            )

				/*
				 * .sessionManagement(session -> session
				 * .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				 * .sessionFixation().migrateSession()
				 * 
				 * // ✅ FIX: prevent loop .invalidSessionStrategy((request, response) -> {
				 * String uri = request.getRequestURI();
				 * System.out.println("uri---------SessionCreationPolicy.IF_REQUIRED---------"+
				 * uri); if (!uri.contains("/login")) { clearSessionCookie(request, response,
				 * applicationCookieService); response.sendRedirect(request.getContextPath() +
				 * "/login?sessionExpired=true"); } })
				 * 
				 * .maximumSessions(1) .maxSessionsPreventsLogin(false)
				 * 
				 * // ✅ FIX: prevent loop .expiredSessionStrategy(event -> { HttpServletRequest
				 * request = event.getRequest(); HttpServletResponse response =
				 * event.getResponse(); String uri = request.getRequestURI();
				 * System.out.println(
				 * "uri---------SessionCreationPolicy.IF_REQUIRED-----not----"+uri); if
				 * (!uri.contains("/login")) { clearSessionCookie(request, response,
				 * applicationCookieService); response.sendRedirect(request.getContextPath() +
				 * "/login?sessionExpired=true"); } }) )
				 */
            
            .sessionManagement(session -> session
            	    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            	    .sessionFixation().migrateSession()
            	    .maximumSessions(1)
            	)
            
            
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(accessDeniedHandler)

                // ✅ FIX: prevent infinite redirect
                .authenticationEntryPoint((req, res, authEx) -> {
                    String uri = req.getRequestURI();
                    if (isApiRequest(req)) {
                        writeUnauthorizedApiResponse(res);
                        return;
                    }
                    if (!uri.equals(req.getContextPath() + "/login")) {
                        res.sendRedirect(req.getContextPath() + "/login?unauthenticated=true");
                    }
                })
            )

				/*
				 * .logout(logout -> logout .logoutUrl("/logout")
				 * .logoutSuccessHandler(logoutSuccessHandler) .invalidateHttpSession(true)
				 * .permitAll() )
				 */

            .logout(logout -> logout
	            	    .logoutUrl("/logout")
	            	    .addLogoutHandler(logoutSuccessHandler)
	            	    .logoutSuccessHandler(logoutSuccessHandler)
            	    .invalidateHttpSession(true)
            	    .clearAuthentication(true)
            	    .deleteCookies("JSESSIONID")
                    .permitAll()
            	)
            
            .headers(headers -> headers
                .httpStrictTransportSecurity(
                        hsts -> hsts
                                .maxAgeInSeconds(31_536_000)
                                .includeSubDomains(true)
                                .preload(false))
                // Frame policies are DENY by default, with a narrow compatibility
                // exception for the application's same-origin invoice preview frames.
                .frameOptions(frame -> frame.disable())
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicyHeader(permissions -> permissions
                        .policy(SecurityHeaderPolicy.PERMISSIONS_POLICY))
                .cacheControl(cache -> {})
                .addHeaderWriter(new CacheControlHeadersWriter())
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                        SecurityHeaderPolicy::allowsSameOriginFraming,
                        new XFrameOptionsHeaderWriter(XFrameOptionsMode.SAMEORIGIN)))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                        new NegatedRequestMatcher(SecurityHeaderPolicy::allowsSameOriginFraming),
                        new XFrameOptionsHeaderWriter(XFrameOptionsMode.DENY)))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                        SecurityHeaderPolicy::allowsSameOriginFraming,
                        new ContentSecurityPolicyHeaderWriter(
                                SecurityHeaderPolicy.SAME_ORIGIN_FRAME_CONTENT_SECURITY_POLICY)))
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                        new NegatedRequestMatcher(SecurityHeaderPolicy::allowsSameOriginFraming),
                        new ContentSecurityPolicyHeaderWriter(SecurityHeaderPolicy.CONTENT_SECURITY_POLICY)))
            );

        return http.build();
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)
                ? requestUri.substring(contextPath.length())
                : requestUri;
        return path.startsWith("/api/");
    }

    private void writeUnauthorizedApiResponse(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Authentication is required.\"}");
    }

    private static final class HttpsRequiredRequestMatcher implements RequestMatcher {

        private final TransportSecurityProperties transportSecurityProperties;

        private HttpsRequiredRequestMatcher(TransportSecurityProperties transportSecurityProperties) {
            this.transportSecurityProperties = transportSecurityProperties;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            return transportSecurityProperties.isHttpsRequiredFor(request);
        }
    }

}
