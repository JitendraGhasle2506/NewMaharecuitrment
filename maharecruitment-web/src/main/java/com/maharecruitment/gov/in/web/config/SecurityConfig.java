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

import com.maharecruitment.gov.in.auth.handler.MySimpleUrlAuthenticationSuccessHandler;
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
                        MySimpleUrlAuthenticationSuccessHandler successHandler,
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
                                                .requestMatchers("/home", "/common/**").authenticated()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .requestMatchers("/hr/**", "/employees/**").hasRole("HR")
                                                .requestMatchers("/agency/**").hasRole("AGENCY")
                                                .requestMatchers("/user/**").hasRole("USER")
                                                .requestMatchers("/stm/**").hasRole("STM")
                                                .requestMatchers("/pm/**").hasRole("PM")
                                                .requestMatchers("/hod1/**").hasRole("HOD1")
                                                .requestMatchers("/hod2/**").hasRole("HOD2")
                                                .requestMatchers("/coo/**").hasAnyRole("COO", "AUDITOR")
                                                .requestMatchers("/employee/**").hasRole("EMPLOYEE")
                                                .requestMatchers("/department/**").hasRole("DEPARTMENT")
                                                .requestMatchers("/auditor/**").hasRole("AUDITOR")

                                                // Existing project module URLs
                                                .requestMatchers("/attendance/**")
                                                .hasAnyRole("ATTENDANCE_MANAGER", "ADMIN")
                                                .requestMatchers("/eservicebook/**")
                                                .hasAnyRole("ESERVICEBOOK_MANAGER", "ADMIN")
                                                .requestMatchers("/pension/**").hasAnyRole("PENSION_MANAGER", "ADMIN")
                                                .requestMatchers("/hrms/**").hasAnyRole("HRMS_MANAGER", "ADMIN")
                                                .requestMatchers("/payroll/**").hasAnyRole("PAYROLL_MANAGER", "ADMIN")
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
                                                                .sendRedirect(req.getContextPath() + "/login?unauthenticated=true")))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessHandler(logoutSuccessHandler)
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .headers(headers -> headers
                                                .httpStrictTransportSecurity(
                                                                hsts -> hsts.includeSubDomains(true).preload(true))
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
