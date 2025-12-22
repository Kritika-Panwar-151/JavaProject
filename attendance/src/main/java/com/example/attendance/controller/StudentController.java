package com.example.attendance.controller;

import com.example.attendance.dto.ApiResponse;
import com.example.attendance.dto.StudentAttendanceRequest;
import com.example.attendance.service.StudentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping("/mark-attendance")
    public ResponseEntity<ApiResponse> markAttendance(
            @RequestBody StudentAttendanceRequest request) {

        return ResponseEntity.ok(
                new ApiResponse(studentService.markAttendance(request))
        );
    }
}
