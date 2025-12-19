package com.example.attendance.controller;

import com.example.attendance.dto.ApiResponse;
import com.example.attendance.dto.GenerateSessionRequest;
import com.example.attendance.dto.TeacherLoginRequest;
import com.example.attendance.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    // Constructor Injection (Spring will inject TeacherService)
    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @RequestBody TeacherLoginRequest request) {

        String status = teacherService.login(request);
        return ResponseEntity.ok(new ApiResponse(status));
    }

    // ---------------- GENERATE CLASS CODE ----------------
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateSession(
            @RequestBody GenerateSessionRequest request) {

        String classCode = teacherService.generateSession(request);
        return ResponseEntity.ok(
                Map.of("classCode", classCode)
        );
    }

    // ---------------- START SESSION ----------------
    @PostMapping("/start/{classCode}")
    public ResponseEntity<ApiResponse> startSession(
            @PathVariable String classCode) {

        teacherService.updateStatus(classCode, "ACTIVE");
        return ResponseEntity.ok(
                new ApiResponse("SESSION_STARTED")
        );
    }

    // ---------------- PAUSE SESSION ----------------
    @PostMapping("/pause/{classCode}")
    public ResponseEntity<ApiResponse> pauseSession(
            @PathVariable String classCode) {

        teacherService.updateStatus(classCode, "PAUSED");
        return ResponseEntity.ok(
                new ApiResponse("SESSION_PAUSED")
        );
    }

    // ---------------- RESUME SESSION ----------------
    @PostMapping("/resume/{classCode}")
    public ResponseEntity<ApiResponse> resumeSession(
            @PathVariable String classCode) {

        teacherService.updateStatus(classCode, "ACTIVE");
        return ResponseEntity.ok(
                new ApiResponse("SESSION_RESUMED")
        );
    }

    // ---------------- END SESSION ----------------
    @PostMapping("/end/{classCode}")
    public ResponseEntity<ApiResponse> endSession(
            @PathVariable String classCode) {

        teacherService.updateStatus(classCode, "ENDED");
        return ResponseEntity.ok(
                new ApiResponse("SESSION_ENDED")
        );
    }
}
