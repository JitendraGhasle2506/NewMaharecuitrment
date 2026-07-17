package com.maharecruitment.gov.in.web.config;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.ClassUtils;

@Configuration
@Profile("local")
@ConditionalOnClass(name = {
        LocalHttpRedirectConnectorConfig.TOMCAT_FACTORY_CLASS_NAME,
        LocalHttpRedirectConnectorConfig.CATALINA_CONNECTOR_CLASS_NAME
})
@ConditionalOnProperty(
        prefix = "app.security.local-http-redirect",
        name = "enabled",
        havingValue = "true")
public class LocalHttpRedirectConnectorConfig {

    static final String TOMCAT_FACTORY_CLASS_NAME =
            "org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory";
    static final String CATALINA_CONNECTOR_CLASS_NAME = "org.apache.catalina.connector.Connector";
    private static final Logger log = LoggerFactory.getLogger(LocalHttpRedirectConnectorConfig.class);
    private static final String TOMCAT_DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

    @Bean
    WebServerFactoryCustomizer<WebServerFactory> localHttpRedirectConnector(
            @Value("${app.security.local-http-redirect.http-port:8777}") int httpPort,
            @Value("${server.port:8443}") int httpsPort) {
        return factory -> addRedirectConnector(factory, httpPort, httpsPort);
    }

    private void addRedirectConnector(WebServerFactory factory, int httpPort, int httpsPort) {
        ClassLoader classLoader = LocalHttpRedirectConnectorConfig.class.getClassLoader();
        try {
            Class<?> tomcatFactoryClass = ClassUtils.forName(TOMCAT_FACTORY_CLASS_NAME, classLoader);
            if (!tomcatFactoryClass.isInstance(factory)) {
                log.debug(
                        "Skipping local HTTP redirect connector because web server factory is {}",
                        factory.getClass().getName());
                return;
            }

            Class<?> connectorClass = ClassUtils.forName(CATALINA_CONNECTOR_CLASS_NAME, classLoader);
            Object connector = createRedirectConnector(connectorClass, httpPort, httpsPort);
            Object connectorArray = Array.newInstance(connectorClass, 1);
            Array.set(connectorArray, 0, connector);

            Method addConnectors = tomcatFactoryClass.getMethod(
                    "addAdditionalTomcatConnectors",
                    connectorArray.getClass());
            addConnectors.invoke(factory, connectorArray);
            log.info(
                    "Registered local HTTP redirect connector. httpPort={}, httpsPort={}",
                    httpPort,
                    httpsPort);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to register local HTTP redirect connector.", ex);
        }
    }

    private Object createRedirectConnector(Class<?> connectorClass, int httpPort, int httpsPort)
            throws ReflectiveOperationException {
        Object connector = connectorClass.getConstructor(String.class).newInstance(TOMCAT_DEFAULT_PROTOCOL);
        connectorClass.getMethod("setScheme", String.class).invoke(connector, "http");
        connectorClass.getMethod("setPort", int.class).invoke(connector, httpPort);
        connectorClass.getMethod("setSecure", boolean.class).invoke(connector, false);
        connectorClass.getMethod("setRedirectPort", int.class).invoke(connector, httpsPort);
        connectorClass.getMethod("setAllowTrace", boolean.class).invoke(connector, false);
        return connector;
    }
}
