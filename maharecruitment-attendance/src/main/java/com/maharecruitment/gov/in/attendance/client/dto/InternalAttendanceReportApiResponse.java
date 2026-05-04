package com.maharecruitment.gov.in.attendance.client.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalAttendanceReportApiResponse {

    private boolean status;

    private String message;

    private List<InternalAttendanceReportApiRow> data = new ArrayList<>();

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<InternalAttendanceReportApiRow> getData() {
        return data;
    }

    public void setData(List<InternalAttendanceReportApiRow> data) {
        this.data = data;
    }
}
