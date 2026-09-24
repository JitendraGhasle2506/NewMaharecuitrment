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
            for (var method : EmployeeHierarchyRepository.class.getDeclaredMethods()) {
                Query query = method.getAnnotation(Query.class);
                if (query != null) assertThatCode(() -> manager.createQuery(query.value())).doesNotThrowAnyException();
            }
        } finally {
            factory.destroy();
        }
    }
}
