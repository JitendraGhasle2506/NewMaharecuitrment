package com.maharecruitment.gov.in.master.dto;

public record ApiResponse<T>(String message, T data) {

    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(message, data);
    }
}

