package com.maharecruitment.gov.in.web.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Connector-level defence in depth for executable Spring Boot deployments. */
@Configuration
public class TomcatHttpMethodConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> disableTomcatTrace() {
        return factory -> factory.addConnectorCustomizers(connector -> connector.setAllowTrace(false));
    }
}
