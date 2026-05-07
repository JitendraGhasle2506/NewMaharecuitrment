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

import com.maharecruitment.gov.in.common.security.ApplicationCookieService;
import com.maharecruitment.gov.in.security.handler.CustomAccessDeniedHandler;
import com.maharecruitment.gov.in.security.handler.CustomLoginFailureHandler;
import com.maharecruitment.gov.in.security.handler.CustomLogoutSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
    SecurityFilterChain filterChain(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider,
            ApplicationCookieService applicationCookieService,
            com.maharecruitment.gov.in.auth.handler.MySimpleUrlAuthenticationSuccessHandler successHandler,
            CustomLoginFailureHandler loginFailureHandler,
            CustomAccessDeniedHandler accessDeniedHandler,
            CustomLogoutSuccessHandler logoutSuccessHandler) throws Exception {

        http.authenticationProvider(authenticationProvider);

        http
            // 🔥 (Optional) disable CSRF temporarily if needed
            .csrf(Customizer.withDefaults())

            .authorizeHttpRequests(auth -> auth

                // ✅ IMPORTANT: keep login FIRST
                .requestMatchers("/login", "/doLogin", "/login/otp", "/login/otp/send").permitAll()
                .requestMatchers("/api/verifications/otp/**").permitAll()

                .requestMatchers(
                        "/", "/index", "/register/**",
                        "/registration**", "/js/**", "/css/**",
                        "/img/**", "/images/**", "/icons/**", "/webjars/**",
                        "/test/**", "/otp/**",
                        "/error", "/error/**"
                ).permitAll()

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
                    .hasAnyAuthority("ROLE_HOD", "ROLE_PM", "ROLE_STM")

                .requestMatchers("/stm/**").hasAuthority("ROLE_STM")
                .requestMatchers("/pm/**").hasAuthority("ROLE_PM")
                .requestMatchers("/hod1/**", "/hod2/**").hasAuthority("ROLE_HOD")

                .requestMatchers("/coo/**")
                    .hasAnyAuthority("ROLE_COO", "ROLE_AUDITOR")

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
            	    .logoutSuccessHandler((request, response, authentication) -> {
            	        HttpSession session = request.getSession(false);
            	        if (session != null) {
            	            session.invalidate();
            	        }
            	        response.sendRedirect(request.getContextPath() + "/login?logout=true");
            	    })
            	    .deleteCookies("JSESSIONID")
            	)
            
            .headers(headers -> headers
                .httpStrictTransportSecurity(
                        hsts -> hsts.includeSubDomains(true).preload(true))
                .frameOptions(frame -> frame.sameOrigin())
                .cacheControl(cache -> {})
                .addHeaderWriter(new CacheControlHeadersWriter())
            );

        return http.build();
    }

    private static void clearSessionCookie(
            HttpServletRequest request,
            HttpServletResponse response,
            ApplicationCookieService applicationCookieService) {
        applicationCookieService.expireManagedCookie(request, response, "JSESSIONID");
    }
}
