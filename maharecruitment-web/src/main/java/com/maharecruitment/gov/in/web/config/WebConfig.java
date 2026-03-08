package com.maharecruitment.gov.in.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.maharecruitment.gov.in.web.interceptor.BreadcrumbInterceptor;
import com.maharecruitment.gov.in.web.interceptor.MenuInterceptor;
import com.maharecruitment.gov.in.web.interceptor.SessionValidationInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private BreadcrumbInterceptor breadcrumbInterceptor;

    @Autowired
    private SessionValidationInterceptor sessionValidationInterceptor;

    @Autowired
    private MenuInterceptor menuInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionValidationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/index", "/login", "/doLogin")
                .excludePathPatterns("/register/**", "/registration**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/icons/**", "/img/**", "/webjars/**")
                .excludePathPatterns("/error/**")
                .excludePathPatterns("/api/**", "/rest/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");

        registry.addInterceptor(menuInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/index", "/login", "/doLogin")
                .excludePathPatterns("/register/**", "/registration**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/icons/**", "/img/**", "/webjars/**")
                .excludePathPatterns("/error/**")
                .excludePathPatterns("/api/**", "/rest/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");

        registry.addInterceptor(breadcrumbInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/rest/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
    }
}
