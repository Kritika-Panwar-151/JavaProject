package com.example.attendance.controller;

import com.example.attendance.dto.ApiResponse;
import com.example.attendance.dto.StudentAttendanceRequest;
import com.example.attendance.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    // Constructor-based Dependency Injection
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    // ---------------- MARK ATTENDANCE ----------------
    @PostMapping("/mark-attendance")
    public ResponseEntity<ApiResponse> markAttendance(
            @RequestBody StudentAttendanceRequest request) {

        String status = studentService.markAttendance(request);
        return ResponseEntity.ok(
                new ApiResponse(status)
        );
    }
}
