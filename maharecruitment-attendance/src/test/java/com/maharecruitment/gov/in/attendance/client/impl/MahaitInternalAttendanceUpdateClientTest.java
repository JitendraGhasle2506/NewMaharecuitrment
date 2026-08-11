package com.maharecruitment.gov.in.attendance.client.impl;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.maharecruitment.gov.in.attendance.config.InternalAttendanceSyncProperties;

class MahaitInternalAttendanceUpdateClientTest {

    private static final String UPDATE_API_URL =
            "https://mahaitattendance.espltestingsite.in/api/third-party/update-attendance";
    private static final String API_BASE_URL = "https://mahaitattendance.espltestingsite.in/api";

    @Test
    void checkInSendsOnlyEmployeeCodeDateAndInTime() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MahaitInternalAttendanceUpdateClient client = new MahaitInternalAttendanceUpdateClient(
                builder.build(),
                enabledProperties());

        server.expect(requestTo(UPDATE_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "employee_code": "MahaIT0693",
                          "date": "2026-08-11",
                          "in_time": "10:00"
                        }
                        """, true))
                .andRespond(withSuccess("{\"status\":true}", MediaType.APPLICATION_JSON));

        client.updateCheckIn(
                "MahaIT0693",
                LocalDate.of(2026, 8, 11),
                LocalTime.of(10, 0, 49));

        server.verify();
    }

    @Test
    void checkOutSendsOnlyEmployeeCodeDateAndOutTime() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MahaitInternalAttendanceUpdateClient client = new MahaitInternalAttendanceUpdateClient(
                builder.build(),
                enabledProperties());

        server.expect(requestTo(UPDATE_API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "employee_code": "MahaIT0693",
                          "date": "2026-08-11",
                          "out_time": "11:01"
                        }
                        """, true))
                .andRespond(withSuccess("{\"status\":true}", MediaType.APPLICATION_JSON));

        client.updateCheckOut(
                "MahaIT0693",
                LocalDate.of(2026, 8, 11),
                LocalTime.of(11, 1, 32));

        server.verify();
    }

    private InternalAttendanceSyncProperties enabledProperties() {
        InternalAttendanceSyncProperties properties = new InternalAttendanceSyncProperties();
        properties.setMobileUpdateEnabled(true);
        properties.setBaseUrl(API_BASE_URL);
        return properties;
    }
}
