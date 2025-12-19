package com.example.attendance.dto;

public class AttendanceRequest {

    private String usn;
    private String classCode;
    private double latitude;
    private double longitude;
    private String deviceId;

    public String getUsn() { return usn; }
    public String getClassCode() { return classCode; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDeviceId() { return deviceId; }
}
