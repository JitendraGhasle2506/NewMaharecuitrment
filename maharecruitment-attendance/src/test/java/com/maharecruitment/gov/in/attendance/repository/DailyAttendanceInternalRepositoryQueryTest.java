package com.maharecruitment.gov.in.attendance.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class DailyAttendanceInternalRepositoryQueryTest {

    @Test
    void cellSummaryDefinesEveryAttendanceCteThatItJoins() {
        String sql = queryFor("summarizeAttendanceByCell");

        assertThat(sql)
                .contains("with internal_present as (")
                .contains("), external_present as (")
                .contains("left join internal_present ip")
                .contains("left join external_present ep");
    }

    @Test
    void departmentSummaryRemainsExternalOnlyWithoutInternalAttendanceDependency() {
        String sql = queryFor("summarizeAttendanceByDepartment");

        assertThat(sql)
                .contains("with external_present as (")
                .contains("recruitment_type, ''))) = 'EXTERNAL'")
                .contains("left join external_present ep")
                .doesNotContain("internal_present")
                .doesNotContain(":attendanceDate");
    }

    private String queryFor(String methodName) {
        Method method = Arrays.stream(DailyAttendanceInternalRepository.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Query query = method.getAnnotation(Query.class);
        assertThat(query).as("@Query on %s", methodName).isNotNull();
        return query.value();
    }
}
