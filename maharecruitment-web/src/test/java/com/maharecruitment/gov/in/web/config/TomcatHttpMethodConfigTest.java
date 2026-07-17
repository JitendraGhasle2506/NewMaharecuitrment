package com.maharecruitment.gov.in.web.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TomcatHttpMethodConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TomcatHttpMethodConfig.class);

    @Test
    void backsOffWhenCatalinaConnectorIsNotAvailable() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(TomcatHttpMethodConfig.CATALINA_CONNECTOR_CLASS_NAME))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("disableTomcatTrace");
                });
    }
}
