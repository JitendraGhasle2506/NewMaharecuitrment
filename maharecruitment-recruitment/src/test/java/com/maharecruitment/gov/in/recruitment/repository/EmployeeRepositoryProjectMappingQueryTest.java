package com.maharecruitment.gov.in.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class EmployeeRepositoryProjectMappingQueryTest {

    @Test
    void assignmentQueryPaginatesOnlyEmployeesWithoutDirectProjectMapping() throws Exception {
        Query query = queryOn("findActiveOnboardedWithoutProjectMapping");
        assertThat(normalize(query.value()))
                .contains("not exists")
                .contains("from employeeprojectmappingentity mapping")
                .contains("mapping.employee = employee");
        assertThat(normalize(query.countQuery())).contains("not exists");
    }

    @Test
    void mappedQuerySupportsProjectNameAndCodeSearch() throws Exception {
        Query query = queryOn("findActiveOnboardedWithProjectMapping");
        assertThat(normalize(query.value()))
                .contains("exists")
                .contains("project.projectname")
                .contains("project.projectcode");
        assertThat(normalize(query.countQuery()))
                .contains("project.projectname")
                .contains("project.projectcode");
    }

    private Query queryOn(String methodName) throws Exception {
        Method method = java.util.Arrays.stream(EmployeeRepository.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        return method.getAnnotation(Query.class);
    }

    private String normalize(String query) {
        return query.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
