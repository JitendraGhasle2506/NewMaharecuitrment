package com.maharecruitment.gov.in.web.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.maharecruitment.gov.in.attendance.dto.EmployeeAttendanceRequestDTO;

class InternalAttendanceRequestTemplateTest {

    @Test
    void attendancePageUsesUnifiedRequestTableForEveryStatusSection() throws IOException {
        String template = new ClassPathResource(
                "templates/attendance/attendance-register-internal.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("All leave, tour, and manual attendance requests waiting for review.")
                .contains("All approved leave, tour, and manual attendance requests.")
                .contains("Rejected or cancelled leave, tour, and manual attendance requests.")
                .contains("attendance/fragments/employee-request-table :: requestTable(${pendingRequests}")
                .contains("attendance/fragments/employee-request-table :: requestTable(${approvedRequests}")
                .contains("attendance/fragments/employee-request-table :: requestTable(${rejectedRequests}")
                .contains("requestType == 'LEAVE'")
                .contains("requestType == 'TOUR'")
                .contains("requestType == 'MANUAL_ATTENDANCE'");
    }

    @Test
    void unifiedRequestFragmentRendersLeaveTourAndManualAttendanceDetails() {
        EmployeeAttendanceRequestDTO leave = request(
                "LEAVE", "Leave", "CL / Full Day", "Personal work", "PENDING");
        EmployeeAttendanceRequestDTO tour = request(
                "TOUR", "Tour", "Full Day", "Official visit", "APPROVED");
        EmployeeAttendanceRequestDTO manual = request(
                "MANUAL_ATTENDANCE", "Manual Attendance", "Attendance correction",
                "Missed biometric punch", "REJECTED");
        manual.setInTime("10:00");
        manual.setOutTime("18:00");

        Context context = new Context();
        context.setVariable("requests", List.of(leave, tour, manual));
        context.setVariable("emptyMessage", "No requests found.");

        String rendered = templateEngine().process(
                "attendance/fragments/employee-request-table",
                context);

        assertThat(rendered)
                .contains("Leave")
                .contains("Tour")
                .contains("Manual Attendance")
                .contains("Personal work")
                .contains("Official visit")
                .contains("Missed biometric punch")
                .contains("In 10:00")
                .contains("Out 18:00")
                .contains("request-status-pending")
                .contains("request-status-approved")
                .contains("request-status-rejected");
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private EmployeeAttendanceRequestDTO request(
            String type,
            String typeLabel,
            String category,
            String details,
            String status) {
        EmployeeAttendanceRequestDTO request = new EmployeeAttendanceRequestDTO();
        request.setRequestType(type);
        request.setRequestTypeLabel(typeLabel);
        request.setCategory(category);
        request.setDetails(details);
        request.setStatus(status);
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 5, 2));
        request.setSubmittedAt(LocalDateTime.of(2026, 4, 25, 10, 30));
        return request;
    }
}
