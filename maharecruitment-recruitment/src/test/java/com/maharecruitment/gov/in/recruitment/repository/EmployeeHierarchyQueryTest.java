package com.maharecruitment.gov.in.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/** Compile JPQL against the real entity model, without connecting to any application database. */
class EmployeeHierarchyQueryTest {
    @Test
    void hierarchyProjectionsCompileAgainstTheEntityModel() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPackagesToScan("com.maharecruitment.gov.in");
        factory.setJpaPropertyMap(Map.of(
                "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect",
                "hibernate.boot.allow_jdbc_metadata_access", "false",
                "hibernate.hbm2ddl.auto", "none"));
        factory.afterPropertiesSet();
        try (var manager = factory.getObject().createEntityManager()) {
            for (var repository : new Class<?>[]{EmployeeHierarchyRepository.class, AdditionalChargeRepository.class}) {
                for (var method : repository.getDeclaredMethods()) {
                    Query query = method.getAnnotation(Query.class);
                    if (query != null) assertThatCode(() -> manager.createQuery(query.value())).doesNotThrowAnyException();
                }
            }
            for (var method : EmployeeReportingMappingRepository.class.getDeclaredMethods()) {
                if (!method.getName().equals("findMappedResourceIds")
                        && !method.getName().equals("deletePrimaryResourceMappings")) continue;
                assertThatCode(() -> manager.createQuery(method.getAnnotation(Query.class).value()))
                        .doesNotThrowAnyException();
            }
            assertThatCode(() -> manager.createQuery(EmployeeRepository.class
                    .getMethod("lockForResourceMapping", Long.class).getAnnotation(Query.class).value()))
                    .doesNotThrowAnyException();
        } finally {
            factory.destroy();
        }
    }
}
