package com.maharecruitment.gov.in.attendance.repository;

import static org.assertj.core.api.Assertions.assertThatCode;
import java.util.Map;
import com.maharecruitment.gov.in.recruitment.repository.AdditionalChargeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

class AdditionalChargeApprovalQueryTest {
    @Test void approvalAndDelegationQueriesCompileAgainstTheEntityModel() {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("com.maharecruitment.gov.in");
        factory.setJpaPropertyMap(Map.of("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.boot.allow_jdbc_metadata_access", "false", "hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
        try (var manager = factory.getObject().createEntityManager()) {
            for (var repository : new Class<?>[]{AdditionalChargeRepository.class, LeaveApplicationRepository.class, TourApplicationRepository.class}) {
                for (var method : repository.getDeclaredMethods()) {
                    Query query = method.getAnnotation(Query.class);
                    if (query == null || query.nativeQuery()) continue;
                    assertThatCode(() -> manager.createQuery(query.value())).as(method.getName()).doesNotThrowAnyException();
                    if (!query.countQuery().isBlank()) assertThatCode(() -> manager.createQuery(query.countQuery())).doesNotThrowAnyException();
                }
            }
        } finally { factory.destroy(); }
    }
}
