package com.maharecruitment.gov.in.web.config;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import db.postmigration.EmployeeMasterOnboardingDateSchemaSupport;

@Component
@ConditionalOnProperty(name = "app.employee-master.schema-guard.enabled", havingValue = "true", matchIfMissing = true)
public class EmployeeMasterSchemaGuardCustomizer implements HibernatePropertiesCustomizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmployeeMasterSchemaGuardCustomizer.class);

    private final DataSource dataSource;
    private final AtomicBoolean applied = new AtomicBoolean(false);

    public EmployeeMasterSchemaGuardCustomizer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        if (!applied.compareAndSet(false, true)) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean changed = EmployeeMasterOnboardingDateSchemaSupport.apply(connection);
            if (changed) {
                LOGGER.info(
                        "Employee master schema guard ensured employee_master.mahait_onboarding_date is available before Hibernate schema update.");
            }
        } catch (Exception ex) {
            LOGGER.warn("Employee master schema guard failed: {}", ex.getMessage(), ex);
        }
    }
}
