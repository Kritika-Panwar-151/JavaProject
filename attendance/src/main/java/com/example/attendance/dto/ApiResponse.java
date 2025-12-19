package com.example.attendance.dto;

public class ApiResponse {

    private String status;

    public ApiResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
