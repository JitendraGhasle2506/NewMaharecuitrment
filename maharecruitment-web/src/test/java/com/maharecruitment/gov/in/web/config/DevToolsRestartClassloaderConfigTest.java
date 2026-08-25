package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DevToolsRestartClassloaderConfigTest {

    @Test
    void keepsInternalModulesInOneRestartClassloader() throws IOException {
        ClassPathResource resource = new ClassPathResource("META-INF/spring-devtools.properties");
        Properties configuration = new Properties();
        try (var inputStream = resource.getInputStream()) {
            configuration.load(inputStream);
        }

        Pattern classDirectoryPattern = Pattern.compile(configuration.getProperty(
                "restart.include.maharecruitment-classes"));
        Pattern jarPattern = Pattern.compile(configuration.getProperty(
                "restart.include.maharecruitment-jars"));

        assertThat(classDirectoryPattern.matcher(
                "file:/E:/workspace/maharecruitment-asset/target/classes/").find()).isTrue();
        assertThat(classDirectoryPattern.matcher(
                "file:/E:/workspace/maharecruitment-recruitment/target/classes/").find()).isTrue();
        assertThat(jarPattern.matcher(
                "file:/repo/maharecruitment-asset-0.0.1-SNAPSHOT.jar").find()).isTrue();
        assertThat(jarPattern.matcher(
                "file:/repo/maharecruitment-recruitment-0.0.1-SNAPSHOT.jar").find()).isTrue();
    }
}
