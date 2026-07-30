package com.maharecruitment.gov.in.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Connector-level defence in depth for executable Spring Boot deployments. */
@Configuration
@ConditionalOnClass(name = {
        TomcatHttpMethodConfig.TOMCAT_FACTORY_CLASS_NAME,
        TomcatHttpMethodConfig.CATALINA_CONNECTOR_CLASS_NAME
})
public class TomcatHttpMethodConfig {

    static final String TOMCAT_FACTORY_CLASS_NAME =
            "org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory";
    static final String CATALINA_CONNECTOR_CLASS_NAME = "org.apache.catalina.connector.Connector";

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> disableTomcatTrace() {
        return factory -> factory.addConnectorCustomizers(connector -> connector.setAllowTrace(false));
    }
}
