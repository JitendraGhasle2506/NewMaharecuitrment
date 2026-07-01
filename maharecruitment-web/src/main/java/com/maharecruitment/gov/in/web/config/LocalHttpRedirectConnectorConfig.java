package com.maharecruitment.gov.in.web.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
@ConditionalOnProperty(
        prefix = "app.security.local-http-redirect",
        name = "enabled",
        havingValue = "true")
public class LocalHttpRedirectConnectorConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> localHttpRedirectConnector(
            @Value("${app.security.local-http-redirect.http-port:8777}") int httpPort,
            @Value("${server.port:8443}") int httpsPort) {
        return factory -> factory.addAdditionalTomcatConnectors(createRedirectConnector(httpPort, httpsPort));
    }

    private Connector createRedirectConnector(int httpPort, int httpsPort) {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        return connector;
    }
}
