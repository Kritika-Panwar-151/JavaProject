package com.example.attendance.controller;

import com.example.attendance.dto.*;
import com.example.attendance.entity.*;
import com.example.attendance.repository.*;
import com.example.attendance.util.DistanceUtil;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/student")
@CrossOrigin
public class StudentController {

    private final TeacherSessionRepository sessionRepo;
    private final StudentDeviceRepository deviceRepo;
    private final AttendanceRepository attendanceRepo;

    public StudentController(TeacherSessionRepository s,
                             StudentDeviceRepository d,
                             AttendanceRepository a) {
        this.sessionRepo = s;
        this.deviceRepo = d;
        this.attendanceRepo = a;
    }

    @PostMapping("/mark-attendance")
    public ApiResponse mark(@RequestBody AttendanceRequest req) {

        TeacherSession session = sessionRepo
                .findByClassCodeAndActive(req.getClassCode(), true)
                .orElse(null);

        if(session == null)
            return new ApiResponse("INVALID_CODE");

        if(attendanceRepo.existsByUsnAndClassCode(
                req.getUsn(), req.getClassCode()))
            return new ApiResponse("ALREADY_MARKED");

        deviceRepo.save(new StudentDevice(
                req.getUsn(),
                req.getDeviceId(),
                req.getClassCode()
        ));

        double dist = DistanceUtil.meters(
                session.getTeacherLat(),
                session.getTeacherLon(),
                req.getLatitude(),
                req.getLongitude()
        );

        if(dist > 35)
            return new ApiResponse("OUT_OF_RANGE");

        AttendanceRecord ar = new AttendanceRecord();
        ar.setUsn(req.getUsn());
        ar.setClassCode(req.getClassCode());
        ar.setDeviceId(req.getDeviceId());
        ar.setLatitude(req.getLatitude());
        ar.setLongitude(req.getLongitude());
        ar.setMarkedAt(LocalDateTime.now());
        ar.setStatus("PRESENT");

        attendanceRepo.save(ar);

        return new ApiResponse("SUCCESS");
    }
}
