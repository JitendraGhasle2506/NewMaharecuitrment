package com.maharecruitment.gov.in.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.CacheControlHeadersWriter;

import com.maharecruitment.gov.in.security.handler.CustomAccessDeniedHandler;
import com.maharecruitment.gov.in.security.handler.CustomLoginFailureHandler;
import com.maharecruitment.gov.in.security.handler.CustomLogoutSuccessHandler;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public static PasswordEncoder passwordEncoder() {
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
        SecurityFilterChain filterChain(
                        HttpSecurity http,
                        DaoAuthenticationProvider authenticationProvider,
                        com.maharecruitment.gov.in.auth.handler.MySimpleUrlAuthenticationSuccessHandler successHandler,
                        CustomLoginFailureHandler loginFailureHandler,
                        CustomAccessDeniedHandler accessDeniedHandler,
                        CustomLogoutSuccessHandler logoutSuccessHandler) throws Exception {

                http.authenticationProvider(authenticationProvider);

                http
                                .csrf(Customizer.withDefaults())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/", "/index", "/login", "/doLogin", "/register/**",
                                                                "/registration**", "/js/**", "/css/**",
                                                                "/img/**", "/images/**", "/icons/**", "/webjars/**",
                                                                "/test/**", "/otp/**",
                                                                "/error", "/error/**")
                                                .permitAll()

                                                // .requestMatchers("/api/master/agencies/**").hasAuthority("ROLE_ADMIN")
                                                // .requestMatchers("/master/agencies/**").hasAuthority("ROLE_ADMIN")
                                                .requestMatchers("/common/mahait-profile/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_HR")
                                                .requestMatchers("/home", "/common/**").authenticated()
                                                .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                                                .requestMatchers("/hr/department/payment/**")
                                                .hasAnyAuthority("ROLE_HR", "ROLE_AUDITOR")
                                                .requestMatchers("/hr/**", "/employees/**").hasAuthority("ROLE_HR")
                                                .requestMatchers("/agency/**").hasAuthority("ROLE_AGENCY")
                                                .requestMatchers("/user/**").hasAuthority("ROLE_USER")
                                                .requestMatchers("/panel/**")
                                                .hasAnyAuthority("ROLE_COO", "ROLE_HOD", "ROLE_STM")
                                                .requestMatchers("/interview-authority/**")
                                                .hasAnyAuthority("ROLE_HOD", "ROLE_PM", "ROLE_STM")
                                                .requestMatchers("/stm/**").hasAuthority("ROLE_STM")
                                                .requestMatchers("/pm/**").hasAuthority("ROLE_PM")
                                                .requestMatchers("/hod1/**", "/hod2/**").hasAuthority("ROLE_HOD")
                                                .requestMatchers("/coo/**").hasAnyAuthority("ROLE_COO", "ROLE_AUDITOR")
                                                .requestMatchers("/employee/**").hasAuthority("ROLE_EMPLOYEE")
                                                .requestMatchers("/department/payment/*/receipt")
                                                .hasAnyAuthority("ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AUDITOR")
                                                .requestMatchers("/invoice/**")
                                                .hasAnyAuthority("ROLE_ADMIN", "ROLE_DEPARTMENT", "ROLE_HR", "ROLE_AUDITOR")
                                                .requestMatchers("/department/**").hasAuthority("ROLE_DEPARTMENT")
                                                .requestMatchers("/auditor/**").hasAuthority("ROLE_AUDITOR")

                                                // Existing project module URLs
                                                .requestMatchers("/attendance/**", "/eservicebook/**", "/pension/**",
                                                                "/hrms/**", "/payroll/**")
                                                .hasAuthority("ROLE_ADMIN")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/doLogin")
                                                .successHandler(successHandler)
                                                .failureHandler(loginFailureHandler)
                                                .permitAll())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                                .sessionFixation().migrateSession()
                                                .invalidSessionStrategy((request, response) -> {
                                                        clearSessionCookie(request, response);
                                                        response.sendRedirect(
                                                                        request.getContextPath()
                                                                                        + "/login?sessionExpired=true");
                                                })
                                                .maximumSessions(1)
                                                .maxSessionsPreventsLogin(false)
                                                .expiredSessionStrategy(event -> {
                                                        HttpServletRequest request = event.getRequest();
                                                        HttpServletResponse response = event.getResponse();
                                                        clearSessionCookie(request, response);
                                                        response.sendRedirect(
                                                                        request.getContextPath()
                                                                                        + "/login?sessionExpired=true");
                                                }))
                                .exceptionHandling(ex -> ex
                                                .accessDeniedHandler(accessDeniedHandler)
                                                .authenticationEntryPoint((req, res, authEx) -> res
                                                                .sendRedirect(req.getContextPath()
                                                                                + "/login?unauthenticated=true")))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessHandler(logoutSuccessHandler)
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .headers(headers -> headers
                                                .httpStrictTransportSecurity(
                                                                hsts -> hsts.includeSubDomains(true).preload(true))
                                                .frameOptions(frame -> frame.sameOrigin())
                                                .cacheControl(cache -> {
                                                })
                                                .addHeaderWriter(new CacheControlHeadersWriter()));

                return http.build();
        }

        private static void clearSessionCookie(HttpServletRequest request, HttpServletResponse response) {
                String contextPath = request.getContextPath();
                String cookiePath = (contextPath == null || contextPath.isBlank()) ? "/" : contextPath;

                Cookie scopedCookie = new Cookie("JSESSIONID", "");
                scopedCookie.setMaxAge(0);
                scopedCookie.setPath(cookiePath);
                scopedCookie.setHttpOnly(true);
                response.addCookie(scopedCookie);

                Cookie rootCookie = new Cookie("JSESSIONID", "");
                rootCookie.setMaxAge(0);
                rootCookie.setPath("/");
                rootCookie.setHttpOnly(true);
                response.addCookie(rootCookie);
        }
}
