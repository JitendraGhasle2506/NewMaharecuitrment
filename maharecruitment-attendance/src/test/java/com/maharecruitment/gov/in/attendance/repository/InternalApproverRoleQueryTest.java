package com.maharecruitment.gov.in.attendance.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Compiles the approver-role and internal-decision-log JPQL against the entity model; no database needed. */
class InternalApproverRoleQueryTest {

    @Test
    void approverRoleAndDecisionLogQueriesCompileAgainstTheEntityModel() {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("com.maharecruitment.gov.in");
        factory.setJpaPropertyMap(Map.of("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.boot.allow_jdbc_metadata_access", "false", "hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
        int compiled = 0;
        try (var manager = factory.getObject().createEntityManager()) {
            for (var repository : new Class<?>[]{InternalApproverRoleRepository.class,
                    AttendanceRequestAuditLogRepository.class}) {
                for (var method : repository.getDeclaredMethods()) {
                    Query query = method.getAnnotation(Query.class);
                    if (query == null || query.nativeQuery()) continue;
                    assertThatCode(() -> manager.createQuery(query.value())).as(method.getName())
                            .doesNotThrowAnyException();
                    if (!query.countQuery().isBlank()) {
                        assertThatCode(() -> manager.createQuery(query.countQuery())).as(method.getName() + " count")
                                .doesNotThrowAnyException();
                    }
                    compiled++;
                }
            }
        } finally {
            factory.destroy();
        }
        assertThat(compiled).isEqualTo(2);
    }
}
