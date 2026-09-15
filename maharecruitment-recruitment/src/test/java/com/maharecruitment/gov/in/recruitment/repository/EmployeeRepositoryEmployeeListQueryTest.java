package com.maharecruitment.gov.in.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

class EmployeeRepositoryEmployeeListQueryTest {

    @Test
    void employeeListSearchIncludesCellAndResolvedProjectManager() throws Exception {
        Method method = EmployeeRepository.class.getMethod(
                "findEmployeeListPageByStatusAndFilters",
                String.class,
                String.class,
                Long.class,
                String.class,
                Pageable.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("upper(coalesce(cell.cellName, '')) like :searchPattern")
                .contains("when manager.employeeId is not null then manager.fullName")
                .contains("else reportingAuthority.name end");
        assertThat(query.countQuery())
                .contains("searchCell.cellName")
                .contains("searchManager.fullName")
                .contains("searchReportingAuthority.name")
                .contains("searchCellAuthority.name");
    }
}
