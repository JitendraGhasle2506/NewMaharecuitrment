package com.maharecruitment.gov.in.attendance.client.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;

class MahaitInternalAttendanceReportClientTest {

    @Test
    void requestUrlDoesNotIncludeOrganizationCode() {
        MahaitInternalAttendanceReportClient client = createClient();

        String requestUrl = client.buildRequestUrl(
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 11));

        assertThat(requestUrl)
                .isEqualTo("https://mahaitattendance.espltestingsite.in/api/third-party/attendance-report-org"
                        + "?start_date=01-08-2026&end_date=11-08-2026")
                .doesNotContain("organization_code", "organizationCode", "ALL");
    }

    @Test
    void parsesCurrentTopLevelArrayResponseAndEmployeCodeField() {
        MahaitInternalAttendanceReportClient client = createClient();
        String responseBody = """
                [
                  {
                    "name": "Aakash Mahendra Tiwari",
                    "Employe_code": "MahaIT3286",
                    "date": "2026-08-03",
                    "in_time": "10:16",
                    "out_time": "18:18",
                    "status": "P"
                  },
                  {
                    "name": "Aakash Mahendra Tiwari",
                    "Employe_code": "MahaIT3286",
                    "date": "2026-08-01",
                    "in_time": null,
                    "out_time": null,
                    "status": "WOF"
                  }
                ]
                """;

        var records = client.parseAttendanceRecords(responseBody);

        assertThat(records).hasSize(2);
        assertThat(records)
                .extracting(record -> record.getAttendanceDate())
                .containsExactly(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3));
        assertThat(records.get(1).getEmployeeName()).isEqualTo("Aakash Mahendra Tiwari");
        assertThat(records.get(1).getUniqueCode()).isEqualTo("MahaIT3286");
        assertThat(records.get(1).getInTime()).isEqualTo("10:16");
        assertThat(records.get(1).getOutTime()).isEqualTo("18:18");
        assertThat(records.get(1).getStatus()).isEqualTo("P");
    }

    @Test
    void continuesToParseLegacyWrappedResponse() {
        MahaitInternalAttendanceReportClient client = createClient();
        String responseBody = """
                {
                  "status": true,
                  "message": "Success",
                  "count": 1,
                  "data": [
                    {
                      "name": "Test Employee",
                      "employee_code": "MahaIT1234",
                      "date": "2026-08-04",
                      "in_time": "09:30",
                      "out_time": "18:30",
                      "status": "P"
                    }
                  ]
                }
                """;

        var records = client.parseAttendanceRecords(responseBody);

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.getUniqueCode()).isEqualTo("MahaIT1234");
            assertThat(record.getAttendanceDate()).isEqualTo(LocalDate.of(2026, 8, 4));
        });
    }

    private MahaitInternalAttendanceReportClient createClient() {
        InternalAttendanceSyncProperties properties = new InternalAttendanceSyncProperties();
        properties.setApiUrl(
                "https://mahaitattendance.espltestingsite.in/api/third-party/attendance-report-org");
        return new MahaitInternalAttendanceReportClient(properties);
    }
}
