package com.example.attendance.controller;

import com.example.attendance.dto.*;
import com.example.attendance.service.TeacherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @RequestBody TeacherLoginRequest request) {

        return ResponseEntity.ok(
                new ApiResponse(teacherService.login(request))
        );
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(
            @RequestBody GenerateSessionRequest request) {

        return ResponseEntity.ok(
                Map.of("classCode", teacherService.generateSession(request))
        );
    }

    @PostMapping("/start/{code}")
    public ResponseEntity<ApiResponse> start(@PathVariable String code) {
        teacherService.updateStatus(code, "ACTIVE");
        return ResponseEntity.ok(new ApiResponse("SESSION_STARTED"));
    }

    @PostMapping("/pause/{code}")
    public ResponseEntity<ApiResponse> pause(@PathVariable String code) {
        teacherService.updateStatus(code, "PAUSED");
        return ResponseEntity.ok(new ApiResponse("SESSION_PAUSED"));
    }

    @PostMapping("/resume/{code}")
    public ResponseEntity<ApiResponse> resume(@PathVariable String code) {
        teacherService.updateStatus(code, "ACTIVE");
        return ResponseEntity.ok(new ApiResponse("SESSION_RESUMED"));
    }

    @PostMapping("/end/{code}")
    public ResponseEntity<ApiResponse> end(@PathVariable String code) {
        teacherService.updateStatus(code, "ENDED");
        return ResponseEntity.ok(new ApiResponse("SESSION_ENDED"));
    }
    @GetMapping("/attendance/{classCode}")
    public ResponseEntity<?> getAttendance(@PathVariable String classCode) {
        return ResponseEntity.ok(
                teacherService.getAttendanceForClass(classCode)
        );
}

}
